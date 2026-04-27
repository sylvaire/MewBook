package com.mewbook.app.data.smartimport

object SmartImportApiPolicy {
    fun chatCompletionsUrl(baseUrl: String): String = "${apiBaseUrl(baseUrl)}/chat/completions"

    fun fileUploadUrl(baseUrl: String): String = "${apiBaseUrl(baseUrl)}/files"

    fun responsesUrl(baseUrl: String): String = "${apiBaseUrl(baseUrl)}/responses"

    fun isSupportedImportFile(fileName: String, mimeType: String?): Boolean {
        val extension = fileName.substringAfterLast('.', missingDelimiterValue = "").lowercase()
        val normalizedMime = mimeType?.substringBefore(';')?.trim()?.lowercase().orEmpty()
        if (extension.isNotBlank()) {
            return extension in SUPPORTED_EXTENSIONS
        }
        return normalizedMime in SUPPORTED_MIME_TYPES
    }

    fun importFileMediaType(fileName: String, mimeType: String?): String {
        val extension = fileName.substringAfterLast('.', missingDelimiterValue = "").lowercase()
        val normalizedMime = mimeType?.substringBefore(';')?.trim()?.lowercase().orEmpty()
        return when {
            extension == "json" -> JSON_MEDIA_TYPE
            extension == "csv" -> CSV_MEDIA_TYPE
            extension == "txt" -> TXT_MEDIA_TYPE
            normalizedMime in JSON_MIME_TYPES -> JSON_MEDIA_TYPE
            normalizedMime in CSV_MIME_TYPES -> CSV_MEDIA_TYPE
            normalizedMime in TXT_MIME_TYPES -> TXT_MEDIA_TYPE
            else -> CSV_MEDIA_TYPE
        }
    }

    private fun apiBaseUrl(baseUrl: String): String {
        val cleaned = baseUrl.trim().trimEnd('/')
        require(cleaned.startsWith("http://") || cleaned.startsWith("https://")) {
            "API 地址必须以 http:// 或 https:// 开头"
        }
        return cleaned
            .removeSuffix("/chat/completions")
            .removeSuffix("/responses")
            .removeSuffix("/files")
            .trimEnd('/')
    }

    private val SUPPORTED_EXTENSIONS = setOf("txt", "csv", "json")
    private val TXT_MIME_TYPES = setOf("text/plain")
    private val CSV_MIME_TYPES = setOf(
        "text/csv",
        "application/csv",
        "text/comma-separated-values",
        "application/vnd.ms-excel"
    )
    private val JSON_MIME_TYPES = setOf("application/json", "text/json")
    private val SUPPORTED_MIME_TYPES = TXT_MIME_TYPES + CSV_MIME_TYPES + JSON_MIME_TYPES
    private const val TXT_MEDIA_TYPE = "text/plain"
    private const val CSV_MEDIA_TYPE = "text/csv"
    private const val JSON_MEDIA_TYPE = "application/json"
}
