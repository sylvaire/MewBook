package com.mewbook.app.data.backup

import java.math.BigDecimal
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter
import java.time.format.DateTimeParseException
import java.util.Locale
import java.util.UUID

object BackupImportPolicy {

    fun parseExternalCsv(csvString: String): BackupEnvelope {
        val rows = parseCsvRows(csvString.removePrefix("\uFEFF"))
        require(rows.size >= 2) { "CSV 内容为空或缺少数据行" }

        val headers = rows.first().map(::normalizeHeader)
        val headerIndex = headers.withIndex().associate { it.value to it.index }

        val dateIndex = requireHeaderIndex(headerIndex, DATE_HEADER_ALIASES, "日期")
        val categoryIndex = requireHeaderIndex(headerIndex, CATEGORY_HEADER_ALIASES, "分类")
        val amountIndex = requireHeaderIndex(headerIndex, AMOUNT_HEADER_ALIASES, "金额")
        val typeIndex = findHeaderIndex(headerIndex, TYPE_HEADER_ALIASES)
        val subCategoryIndex = findHeaderIndex(headerIndex, SUBCATEGORY_HEADER_ALIASES)
        val noteIndex = findHeaderIndex(headerIndex, NOTE_HEADER_ALIASES)
        val accountIndex = findHeaderIndex(headerIndex, ACCOUNT_HEADER_ALIASES)
        val ledgerIndex = findHeaderIndex(headerIndex, LEDGER_HEADER_ALIASES)

        val exportedAt = LocalDateTime.now().format(DateTimeFormatter.ISO_LOCAL_DATE_TIME)
        val ledgers = mutableListOf<BackupLedger>()
        val categories = mutableListOf<BackupCategory>()
        val accounts = mutableListOf<BackupAccount>()
        val records = mutableListOf<BackupRecord>()

        var nextLedgerId = 1L
        var nextCategoryId = 1L
        var nextAccountId = 1L
        val ledgerIdByName = linkedMapOf<String, Long>()
        val categoryIdByKey = linkedMapOf<CategoryPathKey, Long>()
        val accountIdByKey = linkedMapOf<AccountKey, Long>()

        fun ensureLedger(name: String?): Long {
            val rawName = name?.trim().orEmpty().ifBlank { DEFAULT_LEDGER_NAME }
            val key = normalizeName(rawName)
            return ledgerIdByName.getOrPut(key) {
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

        fun ensureCategory(type: String, parentName: String?, name: String): Long {
            val parentId = parentName
                ?.trim()
                ?.takeIf { it.isNotEmpty() }
                ?.let { ensureCategory(type, null, it) }
            val key = CategoryPathKey(
                type = type,
                parentId = parentId,
                normalizedName = normalizeName(name)
            )
            return categoryIdByKey.getOrPut(key) {
                val id = nextCategoryId++
                categories += BackupCategory(
                    id = id,
                    name = name.trim(),
                    icon = defaultCategoryIcon(type, parentId != null),
                    color = defaultCategoryColor(type),
                    type = type,
                    isDefault = false,
                    sortOrder = categories.count { it.type == type && it.parentId == parentId },
                    parentId = parentId
                )
                id
            }
        }

        fun ensureAccount(ledgerId: Long, name: String?): Long? {
            val rawName = name?.trim().orEmpty()
            if (rawName.isBlank()) return null
            val key = AccountKey(ledgerId = ledgerId, normalizedName = normalizeName(rawName))
            return accountIdByKey.getOrPut(key) {
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

        val fallbackTimestamp = System.currentTimeMillis() / 1000
        rows.drop(1).forEachIndexed { index, row ->
            if (row.isEmpty() || row.all { it.isBlank() }) {
                return@forEachIndexed
            }

            val rawDate = row.valueAt(dateIndex)
            val rawCategory = row.valueAt(categoryIndex)
            val rawAmount = row.valueAt(amountIndex)
            if (rawDate.isBlank() || rawCategory.isBlank() || rawAmount.isBlank()) {
                return@forEachIndexed
            }

            val ledgerId = ensureLedger(row.valueAt(ledgerIndex))
            val amount = parseAmount(rawAmount)
            val type = parseType(row.valueAt(typeIndex), amount)
            val categoryId = ensureCategory(
                type = type,
                parentName = row.valueAt(subCategoryIndex).takeIf { it.isNotBlank() }?.let { rawCategory },
                name = row.valueAt(subCategoryIndex).takeIf { it.isNotBlank() } ?: rawCategory
            )
            val accountId = ensureAccount(ledgerId, row.valueAt(accountIndex))
            val parsedDate = parseDate(rawDate)
            val absoluteAmount = amount.abs().toDouble()
            val createdAt = fallbackTimestamp + index

            records += BackupRecord(
                id = (index + 1).toLong(),
                amount = absoluteAmount,
                type = type,
                categoryId = categoryId,
                note = row.valueAt(noteIndex).takeIf { it.isNotBlank() },
                date = parsedDate.toEpochDay(),
                createdAt = createdAt,
                updatedAt = createdAt,
                syncId = UUID.randomUUID().toString(),
                ledgerId = ledgerId,
                accountId = accountId
            )
        }

        require(records.isNotEmpty()) { "没有找到可导入的记录，请检查 CSV 列名和内容" }

        return BackupEnvelope(
            schemaVersion = BackupMigration.CURRENT_SCHEMA_VERSION,
            appVersion = "external-csv-import",
            exportedAt = exportedAt,
            payload = BackupPayload(
                records = records,
                categories = categories,
                accounts = accounts,
                budgets = emptyList(),
                templates = emptyList(),
                ledgers = ledgers
            )
        )
    }

    fun previewRecordImport(
        current: BackupEnvelope,
        incoming: BackupEnvelope
    ): BackupRecordImportPreview {
        return buildRecordImportPlan(current, incoming).preview
    }

    fun mergeRecordImport(
        current: BackupEnvelope,
        incoming: BackupEnvelope
    ): BackupEnvelope {
        return buildRecordImportPlan(current, incoming).mergedEnvelope
    }

    private fun buildRecordImportPlan(
        current: BackupEnvelope,
        incoming: BackupEnvelope
    ): RecordImportPlan {
        val mergedLedgers = current.payload.ledgers.toMutableList()
        val mergedCategories = current.payload.categories.toMutableList()
        val mergedAccounts = current.payload.accounts.toMutableList()
        val mergedRecords = current.payload.records.toMutableList()

        var nextLedgerId = (mergedLedgers.maxOfOrNull { it.id } ?: 0L) + 1L
        var nextCategoryId = (mergedCategories.maxOfOrNull { it.id } ?: 0L) + 1L
        var nextAccountId = (mergedAccounts.maxOfOrNull { it.id } ?: 0L) + 1L
        var nextRecordId = (mergedRecords.maxOfOrNull { it.id } ?: 0L) + 1L

        val ledgerIdMap = mutableMapOf<Long, Long>()
        val categoryIdMap = mutableMapOf<Long, Long>()
        val accountIdMap = mutableMapOf<Long, Long>()
        val ledgerKeyMap = mergedLedgers.associateBy { normalizeName(it.name) }.toMutableMap()
        val categoryKeyMap = mergedCategories.associateBy { categoryKey(it) }.toMutableMap()
        val accountKeyMap = mergedAccounts.associateBy { AccountKey(it.ledgerId, normalizeName(it.name)) }.toMutableMap()
        val accountIndexById = mergedAccounts.withIndex().associate { it.value.id to it.index }.toMutableMap()
        val recordFingerprints = mergedRecords.mapTo(mutableSetOf()) { recordFingerprint(it) }

        var ledgersToCreate = 0
        incoming.payload.ledgers.forEach { incomingLedger ->
            val key = normalizeName(incomingLedger.name)
            val targetLedger = ledgerKeyMap[key] ?: incomingLedger.copy(
                id = nextLedgerId++,
                isDefault = mergedLedgers.isEmpty()
            ).also { created ->
                mergedLedgers += created
                ledgerKeyMap[key] = created
                ledgersToCreate += 1
            }
            ledgerIdMap[incomingLedger.id] = targetLedger.id
        }

        var categoriesToCreate = 0
        incoming.payload.categories
            .sortedWith(compareBy<BackupCategory> { it.parentId != null }.thenBy { it.sortOrder }.thenBy { it.id })
            .forEach { incomingCategory ->
                val targetParentId = incomingCategory.parentId?.let(categoryIdMap::get)
                val key = CategoryPathKey(
                    type = incomingCategory.type,
                    parentId = targetParentId,
                    normalizedName = normalizeName(incomingCategory.name)
                )
                val targetCategory = categoryKeyMap[key] ?: incomingCategory.copy(
                    id = nextCategoryId++,
                    parentId = targetParentId,
                    sortOrder = mergedCategories.count {
                        it.type == incomingCategory.type && it.parentId == targetParentId
                    }
                ).also { created ->
                    mergedCategories += created
                    categoryKeyMap[key] = created
                    categoriesToCreate += 1
                }
                categoryIdMap[incomingCategory.id] = targetCategory.id
            }

        var accountsToCreate = 0
        incoming.payload.accounts.forEach { incomingAccount ->
            val targetLedgerId = ledgerIdMap[incomingAccount.ledgerId] ?: mergedLedgers.firstOrNull()?.id ?: 1L
            val key = AccountKey(targetLedgerId, normalizeName(incomingAccount.name))
            val targetAccount = accountKeyMap[key] ?: incomingAccount.copy(
                id = nextAccountId++,
                balance = 0.0,
                sortOrder = mergedAccounts.count { it.ledgerId == targetLedgerId },
                isDefault = mergedAccounts.none { it.ledgerId == targetLedgerId && it.isDefault },
                ledgerId = targetLedgerId
            ).also { created ->
                mergedAccounts += created
                accountKeyMap[key] = created
                accountIndexById[created.id] = mergedAccounts.lastIndex
                accountsToCreate += 1
            }
            accountIdMap[incomingAccount.id] = targetAccount.id
        }

        var duplicateRecords = 0
        var recordsToImport = 0
        incoming.payload.records.forEach { incomingRecord ->
            val mappedRecord = incomingRecord.copy(
                id = nextRecordId,
                ledgerId = ledgerIdMap[incomingRecord.ledgerId] ?: mergedLedgers.firstOrNull()?.id ?: 1L,
                categoryId = categoryIdMap[incomingRecord.categoryId]
                    ?: error("未找到分类映射: ${incomingRecord.categoryId}"),
                accountId = incomingRecord.accountId?.let { accountIdMap[it] }
            ).let { record ->
                if (record.syncId.isNullOrBlank()) {
                    record.copy(syncId = UUID.randomUUID().toString())
                } else {
                    record
                }
            }
            val fingerprint = recordFingerprint(mappedRecord)
            if (!recordFingerprints.add(fingerprint)) {
                duplicateRecords += 1
                return@forEach
            }

            nextRecordId += 1L
            mergedRecords += mappedRecord
            recordsToImport += 1

            mappedRecord.accountId?.let { accountId ->
                val accountIndex = accountIndexById[accountId] ?: return@let
                val existingAccount = mergedAccounts[accountIndex]
                val balanceChange = if (mappedRecord.type == "INCOME") {
                    mappedRecord.amount
                } else {
                    -mappedRecord.amount
                }
                mergedAccounts[accountIndex] = existingAccount.copy(
                    balance = existingAccount.balance + balanceChange
                )
            }
        }

        return RecordImportPlan(
            preview = BackupRecordImportPreview(
                current = BackupMigration.summarizeEnvelope(current),
                incoming = BackupMigration.summarizeEnvelope(incoming),
                duplicateRecords = duplicateRecords,
                recordsToImport = recordsToImport,
                categoriesToCreate = categoriesToCreate,
                accountsToCreate = accountsToCreate,
                ledgersToCreate = ledgersToCreate
            ),
            mergedEnvelope = current.copy(
                payload = current.payload.copy(
                    records = mergedRecords,
                    categories = mergedCategories,
                    accounts = mergedAccounts,
                    ledgers = mergedLedgers
                )
            )
        )
    }

    private fun recordFingerprint(record: BackupRecord): String {
        return listOf(
            record.ledgerId.toString(),
            record.type,
            record.categoryId.toString(),
            (record.accountId ?: 0L).toString(),
            record.date.toString(),
            normalizeName(record.note.orEmpty()),
            normalizeAmount(record.amount)
        ).joinToString("|")
    }

    private fun normalizeAmount(value: Double): String {
        return BigDecimal.valueOf(value).stripTrailingZeros().toPlainString()
    }

    private fun categoryKey(category: BackupCategory): CategoryPathKey {
        return CategoryPathKey(
            type = category.type,
            parentId = category.parentId,
            normalizedName = normalizeName(category.name)
        )
    }

    private fun parseCsvRows(csv: String): List<List<String>> {
        val rows = mutableListOf<List<String>>()
        val row = mutableListOf<String>()
        val cell = StringBuilder()
        var inQuotes = false
        var index = 0

        while (index < csv.length) {
            val char = csv[index]
            when {
                char == '"' -> {
                    if (inQuotes && index + 1 < csv.length && csv[index + 1] == '"') {
                        cell.append('"')
                        index += 1
                    } else {
                        inQuotes = !inQuotes
                    }
                }

                char == ',' && !inQuotes -> {
                    row += cell.toString().trim()
                    cell.clear()
                }

                (char == '\n' || char == '\r') && !inQuotes -> {
                    row += cell.toString().trim()
                    cell.clear()
                    if (row.any { it.isNotBlank() }) {
                        rows += row.toList()
                    }
                    row.clear()
                    if (char == '\r' && index + 1 < csv.length && csv[index + 1] == '\n') {
                        index += 1
                    }
                }

                else -> cell.append(char)
            }
            index += 1
        }

        row += cell.toString().trim()
        if (row.any { it.isNotBlank() }) {
            rows += row.toList()
        }

        return rows
    }

    private fun parseAmount(rawValue: String): BigDecimal {
        val cleaned = rawValue
            .trim()
            .replace("¥", "")
            .replace("￥", "")
            .replace(",", "")
            .replace(" ", "")
        return cleaned.toBigDecimalOrNull()
            ?: throw IllegalArgumentException("无法解析金额: $rawValue")
    }

    private fun parseDate(rawValue: String): LocalDate {
        val trimmed = rawValue.trim()
        DATE_FORMATTERS.forEach { formatter ->
            runCatching { return LocalDate.parse(trimmed, formatter) }
        }
        DATE_TIME_FORMATTERS.forEach { formatter ->
            runCatching { return LocalDateTime.parse(trimmed, formatter).toLocalDate() }
        }
        return runCatching { LocalDate.parse(trimmed) }
            .getOrElse { throw IllegalArgumentException("无法解析日期: $rawValue") }
    }

    private fun parseType(rawValue: String?, amount: BigDecimal): String {
        val normalized = normalizeHeader(rawValue.orEmpty())
        return when {
            normalized in EXPENSE_TYPE_ALIASES -> "EXPENSE"
            normalized in INCOME_TYPE_ALIASES -> "INCOME"
            amount.signum() < 0 -> "EXPENSE"
            else -> "EXPENSE"
        }
    }

    private fun normalizeHeader(value: String): String {
        return value
            .trim()
            .lowercase(Locale.ROOT)
            .replace("\uFEFF", "")
            .replace(" ", "")
            .replace("_", "")
            .replace("-", "")
    }

    private fun normalizeName(value: String): String {
        return normalizeHeader(value)
    }

    private fun requireHeaderIndex(
        headerIndex: Map<String, Int>,
        aliases: Set<String>,
        label: String
    ): Int {
        return findHeaderIndex(headerIndex, aliases)
            ?: throw IllegalArgumentException("缺少必需列: $label")
    }

    private fun findHeaderIndex(
        headerIndex: Map<String, Int>,
        aliases: Set<String>
    ): Int? {
        return aliases.firstNotNullOfOrNull(headerIndex::get)
    }

    private fun List<String>.valueAt(index: Int?): String {
        if (index == null || index !in indices) return ""
        return this[index]
    }

    private fun defaultCategoryIcon(type: String, isChild: Boolean): String {
        return when {
            type == "INCOME" -> "payments"
            isChild -> "sell"
            else -> "category"
        }
    }

    private fun defaultCategoryColor(type: String): Long {
        return if (type == "INCOME") 0xFF4CAF50 else 0xFFFF6B6B
    }

    private fun defaultAccountMeta(name: String): AccountMeta {
        val normalized = normalizeName(name)
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

    private data class CategoryPathKey(
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

    private data class RecordImportPlan(
        val preview: BackupRecordImportPreview,
        val mergedEnvelope: BackupEnvelope
    )

    private const val DEFAULT_LEDGER_NAME = "我的账本"
    private const val DEFAULT_LEDGER_COLOR = 0xFF4CAF50

    private val DATE_FORMATTERS = listOf(
        DateTimeFormatter.ofPattern("yyyy-MM-dd"),
        DateTimeFormatter.ofPattern("yyyy/M/d"),
        DateTimeFormatter.ofPattern("yyyy/M/dd"),
        DateTimeFormatter.ofPattern("yyyy年M月d日")
    )

    private val DATE_TIME_FORMATTERS = listOf(
        DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss"),
        DateTimeFormatter.ofPattern("yyyy/M/d HH:mm:ss"),
        DateTimeFormatter.ISO_LOCAL_DATE_TIME
    )

    private val DATE_HEADER_ALIASES = setOf("日期", "date", "记账时间", "交易时间", "time")
    private val TYPE_HEADER_ALIASES = setOf("类型", "type", "收支类型", "收入支出")
    private val CATEGORY_HEADER_ALIASES = setOf("分类", "category", "一级分类", "分类名称")
    private val SUBCATEGORY_HEADER_ALIASES = setOf("子分类", "subcategory", "二级分类", "子类")
    private val AMOUNT_HEADER_ALIASES = setOf("金额", "amount", "money", "sum")
    private val NOTE_HEADER_ALIASES = setOf("备注", "note", "memo", "remark", "description")
    private val ACCOUNT_HEADER_ALIASES = setOf("账户", "account", "支付方式", "支付账户", "账户名称")
    private val LEDGER_HEADER_ALIASES = setOf("账本", "ledger", "book", "账簿")

    private val EXPENSE_TYPE_ALIASES = setOf("支出", "expense", "out", "pay", "消费")
    private val INCOME_TYPE_ALIASES = setOf("收入", "income", "in", "到账", "入账")
}
