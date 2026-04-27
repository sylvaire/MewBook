package com.mewbook.app.data.smartimport

import com.mewbook.app.data.backup.BackupAccount
import com.mewbook.app.data.backup.BackupCategory
import com.mewbook.app.data.backup.BackupEnvelope
import com.mewbook.app.data.backup.BackupLedger
import com.mewbook.app.data.backup.BackupMigration
import com.mewbook.app.data.backup.BackupPayload
import com.mewbook.app.data.backup.BackupRecord
import com.mewbook.app.domain.policy.CategorySemanticPolicy
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.decodeFromJsonElement
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.jsonObject
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.util.UUID

object SmartImportPolicy {
    private val json = Json {
        ignoreUnknownKeys = true
        isLenient = true
    }

    fun parseAiResponseToEnvelope(
        response: String,
        now: LocalDateTime = LocalDateTime.now()
    ): BackupEnvelope {
        val records = parseRecords(response)
        require(records.isNotEmpty()) { "AI 没有返回可导入的记录" }

        val exportedAt = now.format(DateTimeFormatter.ISO_LOCAL_DATE_TIME)
        val ledgers = mutableListOf<BackupLedger>()
        val categories = mutableListOf<BackupCategory>()
        val accounts = mutableListOf<BackupAccount>()
        val backupRecords = mutableListOf<BackupRecord>()

        var nextLedgerId = 1L
        var nextCategoryId = 1L
        var nextAccountId = 1L
        val ledgerIdsByName = linkedMapOf<String, Long>()
        val categoryIdsByKey = linkedMapOf<CategoryKey, Long>()
        val accountIdsByKey = linkedMapOf<AccountKey, Long>()
        val fallbackDate = now.toLocalDate()
        val fallbackTimestamp = now.atZone(ZoneId.systemDefault()).toEpochSecond()

        fun ensureLedger(name: String?): Long {
            val rawName = name?.trim().orEmpty().ifBlank { DEFAULT_LEDGER_NAME }
            val key = CategorySemanticPolicy.normalize(rawName)
            return ledgerIdsByName.getOrPut(key) {
                val id = nextLedgerId++
                ledgers += BackupLedger(
                    id = id,
                    name = rawName,
                    type = "PERSONAL",
                    icon = "person",
                    color = DEFAULT_LEDGER_COLOR,
                    createdAt = id,
                    isDefault = ledgers.isEmpty()
                )
                id
            }
        }

        fun ensureCategory(
            type: String,
            parentName: String?,
            name: String,
            semanticLabel: String?,
            proposedIcon: String?
        ): Long {
            val parentId = parentName
                ?.trim()
                ?.takeIf { it.isNotEmpty() }
                ?.let {
                    ensureCategory(
                        type = type,
                        parentName = null,
                        name = it,
                        semanticLabel = null,
                        proposedIcon = null
                    )
                }
            val key = CategoryKey(
                type = type,
                parentId = parentId,
                normalizedName = CategorySemanticPolicy.normalize(name)
            )
            return categoryIdsByKey.getOrPut(key) {
                val id = nextCategoryId++
                categories += BackupCategory(
                    id = id,
                    name = name.trim(),
                    icon = CategorySemanticPolicy.chooseIcon(
                        type = type,
                        categoryName = name,
                        semanticLabel = semanticLabel,
                        isChild = parentId != null,
                        proposedIcon = proposedIcon
                    ),
                    color = defaultCategoryColor(type),
                    type = type,
                    isDefault = false,
                    sortOrder = categories.count { it.type == type && it.parentId == parentId },
                    parentId = parentId,
                    semanticLabel = semanticLabel
                )
                id
            }
        }

        fun ensureAccount(ledgerId: Long, name: String?): Long? {
            val rawName = name?.trim().orEmpty()
            if (rawName.isBlank()) return null
            val key = AccountKey(ledgerId, CategorySemanticPolicy.normalize(rawName))
            return accountIdsByKey.getOrPut(key) {
                val id = nextAccountId++
                val accountMeta = defaultAccountMeta(rawName)
                accounts += BackupAccount(
                    id = id,
                    name = rawName,
                    type = accountMeta.type,
                    balance = 0.0,
                    icon = accountMeta.icon,
                    color = accountMeta.color,
                    isDefault = accounts.none { it.ledgerId == ledgerId && it.isDefault },
                    sortOrder = accounts.count { it.ledgerId == ledgerId },
                    ledgerId = ledgerId
                )
                id
            }
        }

        records.forEachIndexed { index, record ->
            val type = normalizeType(record.type)
            val amount = requireNotNull(record.amount) { "缺少金额" }
            require(amount > 0.0) { "金额必须大于 0" }
            val date = parseDate(record.date, fallbackDate)
            val categoryName = record.category.trim()
            require(categoryName.isNotBlank()) { "缺少分类" }

            val ledgerId = ensureLedger(record.ledger)
            val categoryId = if (record.subCategory.isNullOrBlank()) {
                ensureCategory(
                    type = type,
                    parentName = null,
                    name = categoryName,
                    semanticLabel = record.categorySemantic,
                    proposedIcon = record.icon
                )
            } else {
                ensureCategory(
                    type = type,
                    parentName = categoryName,
                    name = record.subCategory,
                    semanticLabel = record.subCategorySemantic ?: record.categorySemantic,
                    proposedIcon = record.icon
                )
            }
            val accountId = ensureAccount(ledgerId, record.account)
            val createdAt = fallbackTimestamp + index

            backupRecords += BackupRecord(
                id = (index + 1).toLong(),
                amount = amount,
                type = type,
                categoryId = categoryId,
                note = record.note?.trim()?.takeIf { it.isNotBlank() },
                date = date.toEpochDay(),
                createdAt = createdAt,
                updatedAt = createdAt,
                syncId = UUID.randomUUID().toString(),
                ledgerId = ledgerId,
                accountId = accountId
            )
        }

        return BackupEnvelope(
            schemaVersion = BackupMigration.CURRENT_SCHEMA_VERSION,
            appVersion = "smart-import-ai",
            exportedAt = exportedAt,
            payload = BackupPayload(
                records = backupRecords,
                categories = categories,
                accounts = accounts,
                ledgers = ledgers
            )
        )
    }

    private fun parseRecords(response: String): List<AiImportRecord> {
        val payload = extractJsonPayload(response)
        val element = json.parseToJsonElement(payload)
        val array = when (element) {
            is JsonArray -> element
            is JsonObject -> element["records"]?.jsonArray
                ?: element["data"]?.jsonArray
                ?: throw IllegalArgumentException("AI 响应缺少 records 数组")

            else -> throw IllegalArgumentException("AI 响应不是有效的记录数组")
        }
        return array.map { json.decodeFromJsonElement(AiImportRecord.serializer(), it) }
    }

    private fun extractJsonPayload(response: String): String {
        val fenced = Regex("""```(?:json)?\s*([\s\S]*?)```""", RegexOption.IGNORE_CASE)
            .find(response)
            ?.groupValues
            ?.getOrNull(1)
            ?.trim()
        if (!fenced.isNullOrBlank()) {
            return fenced
        }

        val trimmed = response.trim()
        val firstObject = trimmed.indexOf('{').takeIf { it >= 0 } ?: Int.MAX_VALUE
        val firstArray = trimmed.indexOf('[').takeIf { it >= 0 } ?: Int.MAX_VALUE
        val start = minOf(firstObject, firstArray)
        val end = maxOf(trimmed.lastIndexOf('}'), trimmed.lastIndexOf(']'))
        require(start != Int.MAX_VALUE && end >= start) { "AI 响应中没有找到 JSON" }
        return trimmed.substring(start, end + 1)
    }

    private fun normalizeType(rawValue: String?): String {
        val normalized = CategorySemanticPolicy.normalize(rawValue.orEmpty())
        return when (normalized) {
            "expense", "支出", "花费", "消费" -> "EXPENSE"
            "income", "收入", "进账" -> "INCOME"
            else -> throw IllegalArgumentException("无法识别收支类型: $rawValue")
        }
    }

    private fun parseDate(rawValue: String?, fallbackDate: LocalDate): LocalDate {
        val value = rawValue?.trim().orEmpty()
        if (value.isBlank()) {
            return fallbackDate
        }
        return runCatching { LocalDate.parse(value) }
            .getOrElse { throw IllegalArgumentException("无法解析日期: $rawValue") }
    }

    private fun defaultCategoryColor(type: String): Long {
        return if (type == "INCOME") 0xFF4CAF50 else 0xFFFF6B6B
    }

    private fun defaultAccountMeta(name: String): AccountMeta {
        val normalized = CategorySemanticPolicy.normalize(name)
        return when {
            normalized.contains("alipay") || normalized.contains("支付宝") -> AccountMeta("ALIPAY", "alipay", 0xFF1890FF)
            normalized.contains("wechat") || normalized.contains("微信") -> AccountMeta("WECHAT", "wechat", 0xFF07C160)
            normalized.contains("信用") || normalized.contains("credit") -> AccountMeta("CREDIT_CARD", "credit_card", 0xFFE57373)
            normalized.contains("bank") || normalized.contains("银行卡") || normalized.contains("银行") || normalized.contains("储蓄") -> AccountMeta("BANK", "account_balance", 0xFF2196F3)
            normalized.contains("现金") || normalized.contains("cash") -> AccountMeta("CASH", "account_balance_wallet", 0xFF4CAF50)
            normalized.contains("投资") || normalized.contains("基金") || normalized.contains("stock") -> AccountMeta("INVESTMENT", "trending_up", 0xFF8E24AA)
            else -> AccountMeta("OTHER", "account_balance_wallet", 0xFF90A4AE)
        }
    }

    private data class CategoryKey(
        val type: String,
        val parentId: Long?,
        val normalizedName: String
    )

    private data class AccountKey(
        val ledgerId: Long,
        val normalizedName: String
    )

    private data class AccountMeta(
        val type: String,
        val icon: String,
        val color: Long
    )

    private const val DEFAULT_LEDGER_NAME = "我的账本"
    private const val DEFAULT_LEDGER_COLOR = 0xFF4CAF50
}

@Serializable
private data class AiImportRecord(
    val date: String? = null,
    val type: String? = null,
    val amount: Double? = null,
    val category: String = "",
    @SerialName("categorySemantic")
    val categorySemantic: String? = null,
    val subCategory: String? = null,
    @SerialName("subCategorySemantic")
    val subCategorySemantic: String? = null,
    val account: String? = null,
    val ledger: String? = null,
    val note: String? = null,
    val icon: String? = null
)
