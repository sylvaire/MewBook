package com.mewbook.app.data.backup

import com.mewbook.app.domain.policy.CategorySemanticCandidate
import com.mewbook.app.domain.policy.CategorySemanticPolicy
import java.math.BigDecimal
import java.time.Instant
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.OffsetDateTime
import java.time.ZoneId
import java.time.format.DateTimeFormatter
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
        val amountIndex = findHeaderIndex(headerIndex, AMOUNT_HEADER_ALIASES)
        val incomeAmountIndex = findHeaderIndex(headerIndex, INCOME_AMOUNT_HEADER_ALIASES)
        val expenseAmountIndex = findHeaderIndex(headerIndex, EXPENSE_AMOUNT_HEADER_ALIASES)
        require(amountIndex != null || incomeAmountIndex != null || expenseAmountIndex != null) {
            "缺少必需列: 金额"
        }
        val typeIndex = findHeaderIndex(headerIndex, TYPE_HEADER_ALIASES)
        val subCategoryIndex = findHeaderIndex(headerIndex, SUBCATEGORY_HEADER_ALIASES)
        val noteIndex = findHeaderIndex(headerIndex, NOTE_HEADER_ALIASES)
        val accountIndex = findHeaderIndex(headerIndex, ACCOUNT_HEADER_ALIASES)
        val ledgerIndex = findHeaderIndex(headerIndex, LEDGER_HEADER_ALIASES)
        val syncIdIndex = findHeaderIndex(headerIndex, SYNC_ID_HEADER_ALIASES)
        val timestampIndex = findHeaderIndex(headerIndex, TIMESTAMP_HEADER_ALIASES)

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
            val semanticLabel = CategorySemanticPolicy.semanticLabelFor(name, null)
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
                    icon = defaultCategoryIcon(type, name, semanticLabel, parentId != null),
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
            val rawTimestamp = row.valueAt(timestampIndex)
            val rawCategory = row.valueAt(categoryIndex)
            val categoryPath = resolveCategoryPath(rawCategory, row.valueAt(subCategoryIndex))
            if ((rawDate.isBlank() && rawTimestamp.isBlank()) || categoryPath.name.isBlank()) {
                return@forEachIndexed
            }

            val ledgerId = ensureLedger(row.valueAt(ledgerIndex))
            val parsedAmount = resolveAmount(
                row = row,
                amountIndex = amountIndex,
                incomeAmountIndex = incomeAmountIndex,
                expenseAmountIndex = expenseAmountIndex
            ) ?: return@forEachIndexed
            val type = parseType(
                rawValue = row.valueAt(typeIndex),
                amount = parsedAmount.signedAmount,
                typeHint = parsedAmount.typeHint,
                categoryName = categoryPath.name
            )
            val categoryId = ensureCategory(
                type = type,
                parentName = categoryPath.parentName,
                name = categoryPath.name
            )
            val accountId = ensureAccount(ledgerId, row.valueAt(accountIndex))
            val parsedDate = parseDateOrTimestamp(rawDate, rawTimestamp)
            val absoluteAmount = parsedAmount.signedAmount.abs().toDouble()
            val createdAt = parseTimestampSeconds(rawTimestamp) ?: fallbackTimestamp + index
            val syncId = row.valueAt(syncIdIndex).trim().takeIf { it.isNotBlank() } ?: UUID.randomUUID().toString()

            records += BackupRecord(
                id = (index + 1).toLong(),
                amount = absoluteAmount,
                type = type,
                categoryId = categoryId,
                note = row.valueAt(noteIndex).takeIf { it.isNotBlank() },
                date = parsedDate.toEpochDay(),
                createdAt = createdAt,
                updatedAt = createdAt,
                syncId = syncId,
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
        val categoryMappings = mutableListOf<BackupCategoryImportMapping>()
        val incomingCategoryById = incoming.payload.categories.associateBy { it.id }
        incoming.payload.categories
            .sortedWith(compareBy<BackupCategory> { it.parentId != null }.thenBy { it.sortOrder }.thenBy { it.id })
            .forEach { incomingCategory ->
                val targetParentId = incomingCategory.parentId?.let(categoryIdMap::get)
                val sourceParentName = incomingCategory.parentId?.let(incomingCategoryById::get)?.name
                val semanticMatch = CategorySemanticPolicy.resolveExistingCategory(
                    candidates = mergedCategories.map {
                        CategorySemanticCandidate(
                            id = it.id,
                            name = it.name,
                            type = it.type,
                            parentId = it.parentId,
                            semanticLabel = it.semanticLabel
                        )
                    },
                    incomingName = incomingCategory.name,
                    incomingType = incomingCategory.type,
                    incomingSemanticLabel = incomingCategory.semanticLabel,
                    targetParentId = targetParentId
                )
                val targetCategory = semanticMatch
                    ?.let { match -> mergedCategories.firstOrNull { it.id == match.categoryId } }
                    ?: incomingCategory.copy(
                        id = nextCategoryId++,
                        parentId = targetParentId,
                        icon = CategorySemanticPolicy.chooseIcon(
                            type = incomingCategory.type,
                            categoryName = incomingCategory.name,
                            semanticLabel = incomingCategory.semanticLabel,
                            isChild = targetParentId != null,
                            proposedIcon = incomingCategory.icon
                        ),
                        sortOrder = mergedCategories.count {
                            it.type == incomingCategory.type && it.parentId == targetParentId
                        }
                    ).also { created ->
                        mergedCategories += created
                        categoriesToCreate += 1
                    }
                categoryIdMap[incomingCategory.id] = targetCategory.id
                categoryMappings += BackupCategoryImportMapping(
                    sourceName = incomingCategory.name,
                    sourceParentName = sourceParentName,
                    targetName = targetCategory.name,
                    targetParentName = targetCategory.parentId?.let { parentId ->
                        mergedCategories.firstOrNull { it.id == parentId }?.name
                    },
                    type = incomingCategory.type,
                    action = if (semanticMatch != null) {
                        BackupCategoryImportAction.REUSE_EXISTING
                    } else {
                        BackupCategoryImportAction.CREATE_NEW
                    },
                    icon = targetCategory.icon,
                    reason = semanticMatch?.reason ?: "新建分类"
                )
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
                ledgersToCreate = ledgersToCreate,
                categoryMappings = categoryMappings
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

    private fun parseCsvRows(csv: String): List<List<String>> {
        val delimiter = detectDelimiter(csv)
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

                char == delimiter && !inQuotes -> {
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

    private fun detectDelimiter(csv: String): Char {
        val sampleLine = csv.lineSequence().firstOrNull { it.isNotBlank() } ?: return ','
        val candidates = listOf(',', ';', '\t', '|', '，')
        val counts = mutableMapOf<Char, Int>()
        var inQuotes = false
        var index = 0
        while (index < sampleLine.length) {
            val char = sampleLine[index]
            when {
                char == '"' -> {
                    if (inQuotes && index + 1 < sampleLine.length && sampleLine[index + 1] == '"') {
                        index += 1
                    } else {
                        inQuotes = !inQuotes
                    }
                }

                !inQuotes && char in candidates -> {
                    counts[char] = counts.getOrDefault(char, 0) + 1
                }
            }
            index += 1
        }

        val bestCandidate = candidates.maxByOrNull { counts.getOrDefault(it, 0) } ?: ','
        return if (counts.getOrDefault(bestCandidate, 0) > 0) bestCandidate else ','
    }

    private fun parseAmount(rawValue: String): BigDecimal {
        return parseAmountOrNull(rawValue)
            ?: throw IllegalArgumentException("无法解析金额: $rawValue")
    }

    private fun parseAmountOrNull(rawValue: String): BigDecimal? {
        val trimmed = rawValue.trim()
        if (trimmed.isBlank()) {
            return null
        }

        val normalizedParenthesis = trimmed
            .replace("（", "(")
            .replace("）", ")")
        val negativeByParentheses = normalizedParenthesis.startsWith("(") && normalizedParenthesis.endsWith(")")
        val normalized = normalizedParenthesis
            .removePrefix("(")
            .removeSuffix(")")
            .replace("¥", "")
            .replace("￥", "")
            .replace("元", "")
            .replace(",", "")
            .replace("，", "")
            .replace(" ", "")
            .replace("\u00A0", "")
            .replace("＋", "+")
            .replace("−", "-")

        if (normalized.isBlank() || normalized == "--" || normalized == "—" || normalized == "-") {
            return null
        }

        val direct = normalized.toBigDecimalOrNull()
        if (direct != null) {
            return if (negativeByParentheses) direct.negate() else direct
        }

        val extracted = NUMBER_PATTERN.find(normalized)?.value?.toBigDecimalOrNull()
            ?: return null
        return if (negativeByParentheses) extracted.negate() else extracted
    }

    private fun parseDate(rawValue: String): LocalDate {
        val trimmed = rawValue.trim()
        parseNumericDate(trimmed)?.let { return it }
        DATE_FORMATTERS.forEach { formatter ->
            runCatching { return LocalDate.parse(trimmed, formatter) }
        }
        DATE_TIME_FORMATTERS.forEach { formatter ->
            runCatching { return LocalDateTime.parse(trimmed, formatter).toLocalDate() }
        }
        runCatching { return OffsetDateTime.parse(trimmed).toLocalDate() }
        return runCatching { LocalDate.parse(trimmed) }
            .getOrElse { throw IllegalArgumentException("无法解析日期: $rawValue") }
    }

    private fun parseDateOrTimestamp(rawDate: String, rawTimestamp: String): LocalDate {
        if (rawDate.isNotBlank()) {
            return parseDate(rawDate)
        }
        parseDateFromTimestamp(rawTimestamp)?.let { return it }
        throw IllegalArgumentException("无法解析日期: $rawDate")
    }

    private fun parseTimestampSeconds(rawValue: String): Long? {
        val normalized = rawValue.trim()
        if (!NUMERIC_DATE_PATTERN.matches(normalized)) {
            return null
        }
        val value = normalized.toLongOrNull() ?: return null
        return when {
            normalized.length >= 12 -> value / 1000L
            normalized.length in 9..11 -> value
            else -> null
        }
    }

    private fun parseDateFromTimestamp(rawValue: String): LocalDate? {
        return parseTimestampSeconds(rawValue)
            ?.let { Instant.ofEpochSecond(it).atZone(ZoneId.systemDefault()).toLocalDate() }
    }

    private fun parseType(
        rawValue: String?,
        amount: BigDecimal,
        typeHint: String?,
        categoryName: String?
    ): String {
        if (typeHint != null) {
            return typeHint
        }

        val normalized = normalizeHeader(rawValue.orEmpty())
        return when {
            normalized.isNotBlank() && EXPENSE_TYPE_ALIASES.any { normalized.contains(it) } -> "EXPENSE"
            normalized.isNotBlank() && INCOME_TYPE_ALIASES.any { normalized.contains(it) } -> "INCOME"
            INCOME_CATEGORY_HINTS.any { normalizeHeader(categoryName.orEmpty()).contains(it) } -> "INCOME"
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
            .replace("(", "")
            .replace(")", "")
            .replace("（", "")
            .replace("）", "")
            .replace("【", "")
            .replace("】", "")
            .replace("/", "")
            .replace("\\", "")
            .replace(":", "")
            .replace("：", "")
            .replace(".", "")
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

    private fun resolveAmount(
        row: List<String>,
        amountIndex: Int?,
        incomeAmountIndex: Int?,
        expenseAmountIndex: Int?
    ): ParsedAmount? {
        parseAmountOrNull(row.valueAt(amountIndex))?.let {
            return ParsedAmount(signedAmount = it, typeHint = null)
        }

        val expenseAmount = parseAmountOrNull(row.valueAt(expenseAmountIndex))
        val incomeAmount = parseAmountOrNull(row.valueAt(incomeAmountIndex))

        if (expenseAmount != null && expenseAmount.compareTo(BigDecimal.ZERO) != 0) {
            return ParsedAmount(
                signedAmount = expenseAmount.abs().negate(),
                typeHint = "EXPENSE"
            )
        }
        if (incomeAmount != null && incomeAmount.compareTo(BigDecimal.ZERO) != 0) {
            return ParsedAmount(
                signedAmount = incomeAmount.abs(),
                typeHint = "INCOME"
            )
        }

        return when {
            expenseAmount != null -> ParsedAmount(signedAmount = expenseAmount.abs().negate(), typeHint = "EXPENSE")
            incomeAmount != null -> ParsedAmount(signedAmount = incomeAmount.abs(), typeHint = "INCOME")
            else -> null
        }
    }

    private fun resolveCategoryPath(rawCategory: String, rawSubCategory: String): CategoryPath {
        val subCategory = rawSubCategory.trim()
        if (subCategory.isNotBlank()) {
            return CategoryPath(
                parentName = rawCategory.trim().ifBlank { null },
                name = subCategory
            )
        }

        val category = rawCategory.trim()
        if (category.isBlank()) {
            return CategoryPath(parentName = null, name = "")
        }

        val separator = CATEGORY_PATH_SEPARATORS.firstOrNull { category.contains(it) }
        if (separator == null) {
            return CategoryPath(parentName = null, name = category)
        }

        val segments = category.split(separator)
            .map(String::trim)
            .filter(String::isNotBlank)
        if (segments.size < 2) {
            return CategoryPath(parentName = null, name = category)
        }

        return CategoryPath(parentName = segments.first(), name = segments.last())
    }

    private fun parseNumericDate(rawValue: String): LocalDate? {
        val normalized = rawValue.trim()
        if (!NUMERIC_DATE_PATTERN.matches(normalized)) {
            return null
        }

        val value = normalized.toLongOrNull() ?: return null
        return when {
            normalized.length in 5..6 -> runCatching { LocalDate.ofEpochDay(value) }.getOrNull()
            normalized.length in 7..10 && value >= 1_000_000_000L -> runCatching {
                java.time.Instant.ofEpochSecond(value).atZone(ZoneId.systemDefault()).toLocalDate()
            }.getOrNull()
            normalized.length in 11..13 && value >= 100_000_000_000L -> runCatching {
                java.time.Instant.ofEpochMilli(value).atZone(ZoneId.systemDefault()).toLocalDate()
            }.getOrNull()
            else -> null
        }
    }

    private fun defaultCategoryIcon(
        type: String,
        categoryName: String,
        semanticLabel: String?,
        isChild: Boolean
    ): String {
        return CategorySemanticPolicy.chooseIcon(
            type = type,
            categoryName = categoryName,
            semanticLabel = semanticLabel,
            isChild = isChild,
            proposedIcon = null
        )
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

    private data class ParsedAmount(
        val signedAmount: BigDecimal,
        val typeHint: String?
    )

    private data class CategoryPath(
        val parentName: String?,
        val name: String
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
        DateTimeFormatter.ofPattern("yyyy年M月d日"),
        DateTimeFormatter.ofPattern("yyyy.MM.dd"),
        DateTimeFormatter.ofPattern("yyyyMMdd"),
        DateTimeFormatter.ofPattern("M/d/yyyy"),
        DateTimeFormatter.ofPattern("MM/dd/yyyy")
    )

    private val DATE_TIME_FORMATTERS = listOf(
        DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss"),
        DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm"),
        DateTimeFormatter.ofPattern("yyyy/M/d HH:mm:ss"),
        DateTimeFormatter.ofPattern("yyyy/M/d HH:mm"),
        DateTimeFormatter.ofPattern("yyyy.MM.dd HH:mm:ss"),
        DateTimeFormatter.ofPattern("yyyy.MM.dd HH:mm"),
        DateTimeFormatter.ofPattern("yyyy年M月d日 HH:mm:ss"),
        DateTimeFormatter.ofPattern("yyyy年M月d日 HH:mm"),
        DateTimeFormatter.ISO_LOCAL_DATE_TIME
    )

    private val DATE_HEADER_ALIASES = setOf(
        "日期",
        "date",
        "time",
        "datetime",
        "timestamp",
        "记账时间",
        "记账日期",
        "交易时间",
        "交易日期",
        "发生时间",
        "发生日期",
        "时间戳",
        "时间戳毫秒",
        "timestampms"
    )
    private val TYPE_HEADER_ALIASES = setOf("类型", "type", "收支类型", "收入支出")
    private val CATEGORY_HEADER_ALIASES = setOf(
        "分类",
        "category",
        "一级分类",
        "一级类目",
        "类目",
        "分类名称",
        "分类路径",
        "categorypath"
    )
    private val SUBCATEGORY_HEADER_ALIASES = setOf(
        "子分类",
        "subcategory",
        "二级分类",
        "二级类目",
        "子类",
        "子类目"
    )
    private val AMOUNT_HEADER_ALIASES = setOf("金额", "金额元", "amount", "money", "sum", "交易金额", "收支金额")
    private val INCOME_AMOUNT_HEADER_ALIASES = setOf("收入金额", "入账金额", "incomeamount")
    private val EXPENSE_AMOUNT_HEADER_ALIASES = setOf("支出金额", "出账金额", "expenseamount")
    private val NOTE_HEADER_ALIASES = setOf("备注", "note", "memo", "remark", "description", "说明", "附言")
    private val ACCOUNT_HEADER_ALIASES = setOf(
        "账户",
        "account",
        "支付方式",
        "支付账户",
        "支付工具",
        "付款账户",
        "账户名称",
        "钱包"
    )
    private val LEDGER_HEADER_ALIASES = setOf("账本", "ledger", "book", "账簿", "账本名称")
    private val SYNC_ID_HEADER_ALIASES = setOf("uuid", "syncid", "同步id", "记录id", "id", "流水号")
    private val TIMESTAMP_HEADER_ALIASES = setOf(
        "时间戳",
        "时间戳毫秒",
        "timestamp",
        "timestampms",
        "createdat",
        "创建时间",
        "创建时间戳"
    )

    private val EXPENSE_TYPE_ALIASES = setOf("支出", "expense", "out", "pay", "消费", "付款", "扣款", "转出")
    private val INCOME_TYPE_ALIASES = setOf("收入", "income", "in", "到账", "入账", "收款", "转入", "退款")
    private val INCOME_CATEGORY_HINTS = setOf("工资", "薪资", "奖金", "收入", "退款", "分红", "利息")
    private val CATEGORY_PATH_SEPARATORS = listOf("/", "／", ">", "＞", "|", "｜")
    private val NUMBER_PATTERN = Regex("[-+]?\\d+(\\.\\d+)?")
    private val NUMERIC_DATE_PATTERN = Regex("^-?\\d{5,13}$")
}
