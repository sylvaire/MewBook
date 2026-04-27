package com.mewbook.app.data.smartimport

import com.mewbook.app.data.backup.BackupEnvelope
import com.mewbook.app.data.backup.BackupImportPolicy
import com.mewbook.app.data.backup.BackupPayload
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.put
import kotlinx.serialization.json.putJsonArray
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.MultipartBody
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class SmartImportRepository internal constructor(
    private val okHttpClient: OkHttpClient,
    private val loadCredentials: suspend () -> SmartImportCredentials
) {
    @Inject constructor(
        okHttpClient: OkHttpClient,
        configRepository: SmartImportConfigRepository
    ) : this(
        okHttpClient = okHttpClient,
        loadCredentials = configRepository::loadCredentials
    )

    private val json = Json { ignoreUnknownKeys = true }

    suspend fun convertTextToEnvelope(input: String): Result<BackupEnvelope> = withContext(Dispatchers.IO) {
        runCatching {
            val trimmedInput = input.trim()
            require(trimmedInput.isNotBlank()) { "请输入或选择要智能导入的数据" }
            require(trimmedInput.length <= MAX_INPUT_LENGTH) { "内容过长，请分批导入" }

            val credentials = loadCredentials()
            convertTextToEnvelope(
                input = trimmedInput,
                credentials = credentials,
                prefix = "请将以下账单数据转换为指定 JSON："
            )
        }
    }

    private fun convertTextToEnvelope(
        input: String,
        credentials: SmartImportCredentials,
        prefix: String
    ): BackupEnvelope {
        val requestBody = json.encodeToString(
            ChatCompletionRequest(
                model = credentials.model,
                messages = listOf(
                    ChatMessage(
                        role = "system",
                        content = SMART_IMPORT_SYSTEM_PROMPT
                    ),
                    ChatMessage(
                        role = "user",
                        content = "$prefix\n\n$input"
                    )
                )
            )
        )

        val request = Request.Builder()
            .url(SmartImportApiPolicy.chatCompletionsUrl(credentials.baseUrl))
            .header("Authorization", "Bearer ${credentials.apiKey}")
            .header("Content-Type", JSON_MEDIA_TYPE)
            .post(requestBody.toRequestBody(JSON_MEDIA_TYPE.toMediaType()))
            .build()

        okHttpClient.newCall(request).execute().use { response ->
            if (!response.isSuccessful) {
                error("AI 服务返回 ${response.code}")
            }
            val body = response.body?.string().orEmpty()
            val completion = json.decodeFromString(ChatCompletionResponse.serializer(), body)
            val content = completion.choices.firstOrNull()?.message?.content.orEmpty()
            require(content.isNotBlank()) { "AI 服务没有返回转换结果" }
            return SmartImportPolicy.parseAiResponseToEnvelope(content)
        }
    }

    suspend fun convertFileToEnvelope(
        fileName: String,
        mimeType: String?,
        fileBytes: ByteArray
    ): Result<BackupEnvelope> = withContext(Dispatchers.IO) {
        runCatching {
            val cleanedFileName = fileName.trim().ifBlank { "smart-import-data.csv" }
            require(SmartImportApiPolicy.isSupportedImportFile(cleanedFileName, mimeType)) {
                "智能导入文件仅支持 TXT、CSV 或 JSON 格式"
            }
            require(fileBytes.isNotEmpty()) { "文件内容为空" }
            require(fileBytes.size <= MAX_FILE_BYTES) { "文件过大，请分批导入" }

            parseStructuredFileLocally(
                fileName = cleanedFileName,
                mimeType = mimeType,
                fileBytes = fileBytes
            )?.let { return@runCatching it }

            val credentials = loadCredentials()
            try {
                val uploadedFileId = uploadImportFile(
                    credentials = credentials,
                    fileName = cleanedFileName,
                    mimeType = SmartImportApiPolicy.importFileMediaType(cleanedFileName, mimeType),
                    fileBytes = fileBytes
                )
                convertUploadedFileToEnvelope(
                    credentials = credentials,
                    fileName = cleanedFileName,
                    fileId = uploadedFileId
                )
            } catch (error: SmartImportApiException) {
                if (error.code != 404) {
                    throw error
                }
                convertTextFileToEnvelope(
                    credentials = credentials,
                    fileName = cleanedFileName,
                    mimeType = mimeType,
                    fileBytes = fileBytes
                )
            }
        }
    }

    private fun parseStructuredFileLocally(
        fileName: String,
        mimeType: String?,
        fileBytes: ByteArray
    ): BackupEnvelope? {
        if (!isCsvFile(fileName, mimeType)) {
            return null
        }
        val text = fileBytes.toString(Charsets.UTF_8).trimStart('\uFEFF')
        return runCatching { BackupImportPolicy.parseExternalCsv(text) }.getOrNull()
    }

    private fun isCsvFile(fileName: String, mimeType: String?): Boolean {
        val normalizedMime = mimeType?.substringBefore(';')?.trim()?.lowercase().orEmpty()
        return fileName.endsWith(".csv", ignoreCase = true) ||
            normalizedMime in setOf(
                "text/csv",
                "application/csv",
                "text/comma-separated-values",
                "application/vnd.ms-excel"
            )
    }

    private fun convertUploadedFileToEnvelope(
        credentials: SmartImportCredentials,
        fileName: String,
        fileId: String
    ): BackupEnvelope {
        val requestBody = buildResponsesRequestBody(
            model = credentials.model,
            fileName = fileName,
            fileId = fileId
        )

        val request = Request.Builder()
            .url(SmartImportApiPolicy.responsesUrl(credentials.baseUrl))
            .header("Authorization", "Bearer ${credentials.apiKey}")
            .header("Content-Type", JSON_MEDIA_TYPE)
            .post(requestBody.toRequestBody(JSON_MEDIA_TYPE.toMediaType()))
            .build()

        okHttpClient.newCall(request).execute().use { response ->
            if (!response.isSuccessful) {
                throw SmartImportApiException(response.code, "AI 服务返回 ${response.code}")
            }
            val body = response.body?.string().orEmpty()
            val content = extractResponsesOutputText(body)
            require(content.isNotBlank()) { "AI 服务没有返回转换结果" }
            return SmartImportPolicy.parseAiResponseToEnvelope(content)
        }
    }

    private fun convertTextFileToEnvelope(
        credentials: SmartImportCredentials,
        fileName: String,
        mimeType: String?,
        fileBytes: ByteArray
    ): BackupEnvelope {
        val text = fileBytes.toString(Charsets.UTF_8).trimStart('\uFEFF').trim()
        require(text.isNotBlank()) { "文件内容为空" }
        val chunks = splitTextFileForChat(
            fileName = fileName,
            text = text
        )
        val envelopes = chunks.mapIndexed { index, chunk ->
            val chunkPrompt = buildTextFilePrompt(
                fileName = fileName,
                mimeType = mimeType,
                chunk = chunk,
                chunkIndex = index,
                totalChunks = chunks.size
            )
            convertTextToEnvelope(
                input = chunkPrompt,
                credentials = credentials,
                prefix = "当前 AI 服务不支持文件上传。请直接解析下面这个 TXT/CSV/JSON 文本文件内容，并转换为指定 JSON："
            )
        }
        return mergeSmartImportEnvelopes(envelopes)
    }

    private fun splitTextFileForChat(fileName: String, text: String): List<String> {
        val normalizedText = text.replace("\r\n", "\n").replace('\r', '\n')
        if (normalizedText.length <= MAX_TEXT_CHUNK_LENGTH) {
            return listOf(normalizedText)
        }

        val lines = normalizedText.split('\n')
        val hasCsvHeader = fileName.endsWith(".csv", ignoreCase = true) && lines.size > 1
        val header = lines.firstOrNull()?.takeIf { hasCsvHeader }
        val dataLines = if (header == null) lines else lines.drop(1)
        val chunks = mutableListOf<String>()
        var builder = StringBuilder()

        fun hasData(): Boolean {
            return builder.toString().trim().isNotEmpty() && builder.toString().trim() != header
        }

        fun startChunk() {
            builder = StringBuilder()
            if (header != null) {
                builder.appendLine(header)
            }
        }

        fun flushChunk() {
            if (hasData()) {
                chunks += builder.toString().trimEnd()
            }
            startChunk()
        }

        startChunk()
        dataLines.forEach { line ->
            val lineWithBreakLength = line.length + 1
            if (hasData() && builder.length + lineWithBreakLength > MAX_TEXT_CHUNK_LENGTH) {
                flushChunk()
            }
            builder.appendLine(line)
        }
        if (hasData()) {
            chunks += builder.toString().trimEnd()
        }

        return chunks.ifEmpty { listOf(normalizedText.take(MAX_TEXT_CHUNK_LENGTH)) }
    }

    private fun buildTextFilePrompt(
        fileName: String,
        mimeType: String?,
        chunk: String,
        chunkIndex: Int,
        totalChunks: Int
    ): String {
        val chunkNotice = if (totalChunks == 1) {
            "这是完整文件。"
        } else {
            "这是文件第 ${chunkIndex + 1}/$totalChunks 段。只转换本段中出现的交易，不要补全其他段内容。"
        }
        return "文件名：$fileName\n文件类型：${mimeType ?: "未知"}\n$chunkNotice\n文件内容：\n$chunk"
    }

    private fun mergeSmartImportEnvelopes(envelopes: List<BackupEnvelope>): BackupEnvelope {
        require(envelopes.isNotEmpty()) { "AI 没有返回可导入的记录" }
        if (envelopes.size == 1) {
            return envelopes.single()
        }

        var nextLedgerId = 1L
        var nextCategoryId = 1L
        var nextAccountId = 1L
        var nextRecordId = 1L
        val mergedLedgers = mutableListOf<com.mewbook.app.data.backup.BackupLedger>()
        val mergedCategories = mutableListOf<com.mewbook.app.data.backup.BackupCategory>()
        val mergedAccounts = mutableListOf<com.mewbook.app.data.backup.BackupAccount>()
        val mergedRecords = mutableListOf<com.mewbook.app.data.backup.BackupRecord>()

        envelopes.forEach { envelope ->
            val ledgerIdMap = mutableMapOf<Long, Long>()
            envelope.payload.ledgers.forEach { ledger ->
                val newId = nextLedgerId++
                ledgerIdMap[ledger.id] = newId
                mergedLedgers += ledger.copy(id = newId)
            }

            val categoryIdMap = mutableMapOf<Long, Long>()
            envelope.payload.categories.forEach { category ->
                val newId = nextCategoryId++
                categoryIdMap[category.id] = newId
                mergedCategories += category.copy(
                    id = newId,
                    parentId = category.parentId?.let { categoryIdMap[it] }
                )
            }

            val accountIdMap = mutableMapOf<Long, Long>()
            envelope.payload.accounts.forEach { account ->
                val newId = nextAccountId++
                accountIdMap[account.id] = newId
                mergedAccounts += account.copy(
                    id = newId,
                    ledgerId = ledgerIdMap[account.ledgerId] ?: account.ledgerId
                )
            }

            envelope.payload.records.forEach { record ->
                mergedRecords += record.copy(
                    id = nextRecordId++,
                    categoryId = categoryIdMap[record.categoryId] ?: record.categoryId,
                    ledgerId = ledgerIdMap[record.ledgerId] ?: record.ledgerId,
                    accountId = record.accountId?.let { accountIdMap[it] }
                )
            }
        }

        val first = envelopes.first()
        return first.copy(
            payload = BackupPayload(
                records = mergedRecords,
                categories = mergedCategories,
                accounts = mergedAccounts,
                ledgers = mergedLedgers
            )
        )
    }

    private fun uploadImportFile(
        credentials: SmartImportCredentials,
        fileName: String,
        mimeType: String,
        fileBytes: ByteArray
    ): String {
        val requestBody = MultipartBody.Builder()
            .setType(MultipartBody.FORM)
            .addFormDataPart("purpose", "user_data")
            .addFormDataPart(
                "file",
                fileName,
                fileBytes.toRequestBody(mimeType.toMediaType())
            )
            .build()

        val request = Request.Builder()
            .url(SmartImportApiPolicy.fileUploadUrl(credentials.baseUrl))
            .header("Authorization", "Bearer ${credentials.apiKey}")
            .post(requestBody)
            .build()

        okHttpClient.newCall(request).execute().use { response ->
            if (!response.isSuccessful) {
                throw SmartImportApiException(response.code, "上传文件失败，AI 服务返回 ${response.code}")
            }
            val body = response.body?.string().orEmpty()
            val uploadResponse = json.decodeFromString(FileUploadResponse.serializer(), body)
            return uploadResponse.id.takeIf { it.isNotBlank() }
                ?: error("AI 服务没有返回文件 ID")
        }
    }

    private fun buildResponsesRequestBody(
        model: String,
        fileName: String,
        fileId: String
    ): String {
        val payload = buildJsonObject {
            put("model", model)
            put("instructions", SMART_IMPORT_SYSTEM_PROMPT)
            putJsonArray("input") {
                add(
                    buildJsonObject {
                        put("role", "user")
                        putJsonArray("content") {
                            add(
                                buildJsonObject {
                                    put("type", "input_text")
                                    put(
                                        "text",
                                        "请读取上传的 $fileName，将其中的账单数据转换为指定 JSON。文件可能来自其他记账 App 导出的 TXT、CSV 或 JSON。"
                                    )
                                }
                            )
                            add(
                                buildJsonObject {
                                    put("type", "input_file")
                                    put("file_id", fileId)
                                }
                            )
                        }
                    }
                )
            }
        }
        return json.encodeToString(payload)
    }

    private fun extractResponsesOutputText(body: String): String {
        val response = json.decodeFromString(ResponsesApiResponse.serializer(), body)
        val directText = response.outputText.orEmpty().trim()
        if (directText.isNotBlank()) {
            return directText
        }
        return response.output
            .flatMap { it.content }
            .mapNotNull { it.text?.trim()?.takeIf(String::isNotBlank) }
            .joinToString(separator = "\n")
    }

    private companion object {
        const val MAX_INPUT_LENGTH = 100_000
        const val MAX_TEXT_CHUNK_LENGTH = 80_000
        const val MAX_FILE_BYTES = 10 * 1024 * 1024
        const val JSON_MEDIA_TYPE = "application/json; charset=utf-8"
        const val SMART_IMPORT_SYSTEM_PROMPT = """
你是喵喵记账的数据转换助手。请把用户提供的账单文本转换为纯 JSON，不要输出 Markdown、解释或额外文字。
返回格式必须是：
{
  "records": [
    {
      "date": "yyyy-MM-dd，可选；原始数据没有日期或时间时留空",
      "type": "EXPENSE 或 INCOME",
      "amount": 正数,
      "category": "一级分类或主要分类",
      "categorySemantic": "food/transport/housing/salary/refund/investment/shopping/medical/education/entertainment 等英文语义标签",
      "subCategory": "可选二级分类",
      "subCategorySemantic": "可选二级分类英文语义标签",
      "account": "可选账户",
      "ledger": "可选账本",
      "note": "可选备注",
      "icon": "可选 Material 图标名称"
    }
  ]
}
规则：amount 永远为正数；支出用 EXPENSE，收入用 INCOME；无法确定的备注留空；没有日期或时间时不要编造，date 留空；不要编造不存在的交易。
"""
    }
}

@Serializable
private data class ChatCompletionRequest(
    val model: String,
    val messages: List<ChatMessage>,
    val temperature: Double = 0.0,
    val stream: Boolean = false
)

@Serializable
private data class ChatMessage(
    val role: String,
    val content: String
)

@Serializable
private data class ChatCompletionResponse(
    val choices: List<ChatCompletionChoice> = emptyList()
)

@Serializable
private data class ChatCompletionChoice(
    val message: ChatCompletionMessage? = null
)

@Serializable
private data class ChatCompletionMessage(
    val content: String? = null,
    @SerialName("refusal")
    val refusal: String? = null
)

private class SmartImportApiException(
    val code: Int,
    message: String
) : IllegalStateException(message)

@Serializable
private data class FileUploadResponse(
    val id: String = ""
)

@Serializable
private data class ResponsesApiResponse(
    @SerialName("output_text")
    val outputText: String? = null,
    val output: List<ResponsesOutputItem> = emptyList()
)

@Serializable
private data class ResponsesOutputItem(
    val content: List<ResponsesContentItem> = emptyList()
)

@Serializable
private data class ResponsesContentItem(
    val text: String? = null
)
