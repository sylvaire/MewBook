package com.mewbook.app.data.backup

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.intOrNull
import kotlinx.serialization.json.jsonPrimitive
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.ZoneOffset
import java.util.UUID

object BackupMigration {
    const val CURRENT_SCHEMA_VERSION = 4

    private val json = Json {
        ignoreUnknownKeys = true
        prettyPrint = true
    }

    fun parseToCurrentEnvelope(jsonString: String): BackupEnvelope {
        val trimmed = jsonString.trimStart { it.isWhitespace() || it == '\uFEFF' }
        if (!trimmed.startsWith("{")) {
            return BackupImportPolicy.parseExternalCsv(jsonString)
        }

        val root = json.parseToJsonElement(jsonString) as? JsonObject
            ?: throw IllegalArgumentException("Invalid backup file")

        val schemaVersion = root["schemaVersion"]?.jsonPrimitive?.intOrNull
        if (schemaVersion != null) {
            require(schemaVersion <= CURRENT_SCHEMA_VERSION) {
                "Backup schema v$schemaVersion is newer than supported v$CURRENT_SCHEMA_VERSION"
            }
            val envelope = json.decodeFromString<BackupEnvelope>(jsonString)
            return envelope.copy(
                schemaVersion = CURRENT_SCHEMA_VERSION,
                payload = envelope.payload.copy(
                    records = envelope.payload.records,
                    categories = envelope.payload.categories,
                    accounts = envelope.payload.accounts,
                    budgets = envelope.payload.budgets.map(::normalizeBudget),
                    ledgers = if (envelope.payload.ledgers.isEmpty()) defaultLedgers() else envelope.payload.ledgers
                )
            )
        }

        val versionElement = root["version"]?.jsonPrimitive
        return when {
            versionElement?.content == "1.0" -> migrateLegacyExportV1(
                json.decodeFromString<LegacyExportV1>(jsonString)
            )

            versionElement?.intOrNull == 2 -> migrateLegacyDavV2(
                json.decodeFromString<LegacyDavExportData>(jsonString)
            )

            else -> throw IllegalArgumentException("Unsupported backup format")
        }
    }

    fun encodeEnvelope(envelope: BackupEnvelope): String {
        return json.encodeToString(BackupEnvelope.serializer(), envelope)
    }

    fun summarizeEnvelope(envelope: BackupEnvelope): BackupSnapshotSummary {
        return BackupSnapshotSummary(
            records = envelope.payload.records.size,
            categories = envelope.payload.categories.size,
            accounts = envelope.payload.accounts.size,
            budgets = envelope.payload.budgets.size,
            templates = envelope.payload.templates.size,
            ledgers = envelope.payload.ledgers.size,
            hasDavConfig = envelope.payload.davConfig != null,
            themeMode = envelope.payload.themeMode
        )
    }

    fun compareEnvelopes(
        current: BackupEnvelope,
        incoming: BackupEnvelope
    ): BackupRestorePreview {
        return BackupRestorePreview(
            current = summarizeEnvelope(current),
            incoming = summarizeEnvelope(incoming),
            conflicts = BackupConflictSummary(
                records = countConflicts(current.payload.records.map { it.id }, incoming.payload.records.map { it.id }),
                categories = countConflicts(current.payload.categories.map { it.id }, incoming.payload.categories.map { it.id }),
                accounts = countConflicts(current.payload.accounts.map { it.id }, incoming.payload.accounts.map { it.id }),
                budgets = countConflicts(current.payload.budgets.map { it.id }, incoming.payload.budgets.map { it.id }),
                templates = countConflicts(current.payload.templates.map { it.id }, incoming.payload.templates.map { it.id }),
                ledgers = countConflicts(current.payload.ledgers.map { it.id }, incoming.payload.ledgers.map { it.id })
            )
        )
    }

    private fun normalizeBudget(budget: BackupBudget): BackupBudget {
        val resolvedPeriodType = budget.periodType.ifBlank { "MONTH" }
        val resolvedPeriodKey = budget.periodKey ?: budget.month ?: ""
        return budget.copy(
            periodType = resolvedPeriodType,
            periodKey = resolvedPeriodKey,
            month = if (resolvedPeriodType == "MONTH") resolvedPeriodKey else budget.month
        )
    }

    private fun countConflicts(currentIds: List<Long>, incomingIds: List<Long>): Int {
        if (currentIds.isEmpty() || incomingIds.isEmpty()) {
            return 0
        }
        return currentIds.toSet().intersect(incomingIds.toSet()).size
    }

    private fun migrateLegacyExportV1(legacy: LegacyExportV1): BackupEnvelope {
        val categories = mutableListOf<BackupCategory>()
        val categoryIds = mutableMapOf<Triple<String, String?, String>, Long>()
        var nextCategoryId = 1L

        fun ensureCategory(type: String, parentName: String?, name: String): Long {
            val key = Triple(type, parentName, name)
            return categoryIds.getOrPut(key) {
                val parentId = if (parentName == null) {
                    null
                } else {
                    ensureCategory(type, null, parentName)
                }
                val id = nextCategoryId++
                categories += BackupCategory(
                    id = id,
                    name = name,
                    icon = "more_horiz",
                    color = 0xFF9E9E9E,
                    type = type,
                    isDefault = false,
                    sortOrder = categories.count { it.type == type && it.parentId == parentId },
                    parentId = parentId
                )
                id
            }
        }

        val fallbackTimestamp = parseExportTime(legacy.exportTime)
        val records = legacy.records.mapIndexed { index, record ->
            val type = record.type
            val categoryId = if (record.subCategoryName.isNullOrBlank()) {
                ensureCategory(type, null, record.categoryName.ifBlank { "未分类" })
            } else {
                ensureCategory(type, record.categoryName.ifBlank { "未分类" }, record.subCategoryName)
            }

            BackupRecord(
                id = (index + 1).toLong(),
                amount = record.amount,
                type = type,
                categoryId = categoryId,
                note = record.note?.ifBlank { null },
                date = LocalDate.parse(record.date).toEpochDay(),
                createdAt = fallbackTimestamp + index,
                updatedAt = fallbackTimestamp + index,
                syncId = UUID.randomUUID().toString(),
                ledgerId = 1L,
                accountId = null
            )
        }

        return BackupEnvelope(
            schemaVersion = CURRENT_SCHEMA_VERSION,
            appVersion = "legacy-export-v1",
            exportedAt = legacy.exportTime,
            payload = BackupPayload(
                records = records,
                categories = categories,
                templates = emptyList(),
                ledgers = defaultLedgers()
            )
        )
    }

    private fun migrateLegacyDavV2(legacy: LegacyDavExportData): BackupEnvelope {
        val ledgerIds = legacy.records.map { it.ledgerId }.distinct().ifEmpty { listOf(1L) }
        val ledgers = ledgerIds.mapIndexed { index, ledgerId ->
            BackupLedger(
                id = ledgerId,
                name = if (index == 0) "我的账本" else "导入账本$ledgerId",
                type = "PERSONAL",
                icon = "person",
                color = 0xFF4CAF50,
                createdAt = index.toLong() + 1L,
                isDefault = index == 0
            )
        }

        return BackupEnvelope(
            schemaVersion = CURRENT_SCHEMA_VERSION,
            appVersion = "legacy-dav-v2",
            exportedAt = legacy.exportTime,
            payload = BackupPayload(
                records = legacy.records.map {
                    BackupRecord(
                        id = it.id,
                        amount = it.amount,
                        type = it.type,
                        categoryId = it.categoryId,
                        note = it.note,
                        date = it.date,
                        createdAt = it.createdAt,
                        updatedAt = it.updatedAt,
                        syncId = it.syncId,
                        ledgerId = it.ledgerId,
                        accountId = it.accountId
                    )
                },
                categories = legacy.categories.map {
                    BackupCategory(
                        id = it.id,
                        name = it.name,
                        icon = it.icon,
                        color = it.color,
                        type = it.type,
                        isDefault = it.isDefault,
                        sortOrder = it.sortOrder,
                        parentId = it.parentId
                    )
                },
                templates = emptyList(),
                ledgers = ledgers
            )
        )
    }

    private fun parseExportTime(value: String): Long {
        return runCatching {
            LocalDateTime.parse(value).toEpochSecond(ZoneOffset.UTC)
        }.getOrElse {
            System.currentTimeMillis() / 1000
        }
    }

    private fun defaultLedgers(): List<BackupLedger> {
        return listOf(
            BackupLedger(
                id = 1L,
                name = "我的账本",
                type = "PERSONAL",
                icon = "person",
                color = 0xFF4CAF50,
                createdAt = 1L,
                isDefault = true
            )
        )
    }
}

@Serializable
private data class LegacyExportV1(
    val exportTime: String,
    val version: String,
    val records: List<LegacyExportV1Record>
)

@Serializable
private data class LegacyExportV1Record(
    val date: String,
    val type: String,
    val categoryName: String,
    val subCategoryName: String = "",
    val amount: Double,
    val note: String? = null
)

@Serializable
private data class LegacyDavExportData(
    val version: Int,
    val exportTime: String,
    val records: List<LegacyDavRecord>,
    val categories: List<LegacyDavCategory>
)

@Serializable
private data class LegacyDavRecord(
    val id: Long,
    val amount: Double,
    val type: String,
    val categoryId: Long,
    val note: String? = null,
    val date: Long,
    val createdAt: Long,
    val updatedAt: Long,
    val syncId: String,
    val ledgerId: Long,
    val accountId: Long? = null
)

@Serializable
private data class LegacyDavCategory(
    val id: Long,
    val name: String,
    val icon: String,
    val color: Long,
    val type: String,
    @SerialName("isDefault")
    val isDefault: Boolean,
    val sortOrder: Int,
    val parentId: Long? = null
)
