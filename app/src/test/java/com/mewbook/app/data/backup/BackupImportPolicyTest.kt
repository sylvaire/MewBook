package com.mewbook.app.data.backup

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Assume.assumeTrue
import org.junit.Test
import java.io.File
import java.time.LocalDate

class BackupImportPolicyTest {

    @Test
    fun parseExternalCsv_supportsCommonBookkeepingHeaders() {
        val csv = """
            日期,类型,分类,子分类,金额,备注,账户
            2026-04-20,支出,餐饮,早餐,18.50,豆浆油条,支付宝
            2026-04-21,收入,工资,,5000,四月工资,银行卡
        """.trimIndent()

        val envelope = BackupImportPolicy.parseExternalCsv(csv)

        assertEquals(2, envelope.payload.records.size)
        assertEquals(3, envelope.payload.categories.size)
        assertEquals(2, envelope.payload.accounts.size)
        assertTrue(envelope.payload.records.any { it.note == "豆浆油条" })
        assertTrue(envelope.payload.records.any { it.type == "INCOME" })
    }

    @Test
    fun previewAndMerge_skipDuplicatesAndKeepNewRecords() {
        val current = BackupMigration.parseToCurrentEnvelope(
            """
                {
                  "schemaVersion": 4,
                  "appVersion": "1.0.0",
                  "exportedAt": "2026-04-21T10:00:00",
                  "payload": {
                    "records": [
                      {
                        "id": 1,
                        "amount": 18.5,
                        "type": "EXPENSE",
                        "categoryId": 2,
                        "note": "豆浆油条",
                        "date": 20563,
                        "createdAt": 1713600000,
                        "updatedAt": 1713600000,
                        "syncId": "sync-1",
                        "ledgerId": 1,
                        "accountId": 1
                      }
                    ],
                    "categories": [
                      {
                        "id": 1,
                        "name": "餐饮",
                        "icon": "restaurant",
                        "color": 4294925235,
                        "type": "EXPENSE",
                        "isDefault": true,
                        "sortOrder": 0,
                        "parentId": null
                      },
                      {
                        "id": 2,
                        "name": "早餐",
                        "icon": "restaurant",
                        "color": 4294925235,
                        "type": "EXPENSE",
                        "isDefault": true,
                        "sortOrder": 0,
                        "parentId": 1
                      }
                    ],
                    "accounts": [
                      {
                        "id": 1,
                        "name": "支付宝",
                        "type": "ALIPAY",
                        "balance": -18.5,
                        "icon": "alipay",
                        "color": 4279939327,
                        "isDefault": true,
                        "sortOrder": 0,
                        "ledgerId": 1
                      }
                    ],
                    "budgets": [],
                    "templates": [],
                    "ledgers": [
                      {
                        "id": 1,
                        "name": "我的账本",
                        "type": "PERSONAL",
                        "icon": "person",
                        "color": 4283215696,
                        "createdAt": 1,
                        "isDefault": true
                      }
                    ]
                  }
                }
            """.trimIndent()
        )
        val incoming = BackupImportPolicy.parseExternalCsv(
            """
                日期,类型,分类,子分类,金额,备注,账户
                2026-04-20,支出,餐饮,早餐,18.50,豆浆油条,支付宝
                2026-04-21,支出,餐饮,午餐,28.00,黄焖鸡,支付宝
            """.trimIndent()
        )

        val preview = BackupImportPolicy.previewRecordImport(current, incoming)
        val merged = BackupImportPolicy.mergeRecordImport(current, incoming)

        assertEquals(1, preview.duplicateRecords)
        assertEquals(1, preview.recordsToImport)
        assertEquals(1, preview.categoriesToCreate)
        assertEquals(2, merged.payload.records.size)
        assertTrue(merged.payload.categories.any { it.name == "午餐" })
    }

    @Test
    fun parseExternalCsv_supportsSeparatedIncomeExpenseColumnsAndSemicolonDelimiter() {
        val csv = """
            交易时间;一级分类;二级分类;支出金额;收入金额;账户名称;备注
            2026/4/20 08:30:00;餐饮;早餐;18.50;;微信;豆浆
            2026/4/21 09:00:00;工资;; ;5000;银行卡;四月工资
        """.trimIndent()

        val envelope = BackupImportPolicy.parseExternalCsv(csv)

        assertEquals(2, envelope.payload.records.size)
        assertTrue(envelope.payload.records.any { it.type == "EXPENSE" && it.amount == 18.5 })
        assertTrue(envelope.payload.records.any { it.type == "INCOME" && it.amount == 5000.0 })
    }

    @Test
    fun parseExternalCsv_supportsCategoryPathEpochDayAndFullWidthSeparators() {
        val date = LocalDate.of(2026, 4, 21)
        val csv = """
            记账日期，分类路径，金额(元)，备注，账户
            ${date.toEpochDay()}，餐饮/晚餐，（32.80），测试，现金
        """.trimIndent()

        val envelope = BackupImportPolicy.parseExternalCsv(csv)

        assertEquals(1, envelope.payload.records.size)
        val record = envelope.payload.records.first()
        assertEquals("EXPENSE", record.type)
        assertEquals(32.8, record.amount, 0.0)
        assertEquals(date.toEpochDay(), record.date)
        assertTrue(envelope.payload.categories.any { it.name == "餐饮" && it.parentId == null })
        assertTrue(envelope.payload.categories.any { it.name == "晚餐" && it.parentId != null })
    }

    @Test
    fun parseExternalCsv_supportsCompactNumericDateFormat() {
        val csv = """
            日期,分类,金额
            20260421,餐饮,32.8
        """.trimIndent()

        val envelope = BackupImportPolicy.parseExternalCsv(csv)
        val record = envelope.payload.records.first()

        assertEquals(LocalDate.of(2026, 4, 21).toEpochDay(), record.date)
    }

    @Test
    fun parseExternalCsv_supportsChineseDateTimeWithoutSeconds() {
        val csv = """
            日期,分类,金额
            2020年08月15日 14:11,餐饮,32.8
        """.trimIndent()

        val envelope = BackupImportPolicy.parseExternalCsv(csv)
        val record = envelope.payload.records.first()

        assertEquals(LocalDate.of(2020, 8, 15).toEpochDay(), record.date)
    }

    @Test
    fun parseExternalCsv_supportsUuidMillisTimestampAndQuotedCommaNotes() {
        val csv = """
            UUID,金额,分类,类型,日期,时间戳（毫秒）,备注
            58009EF6-3A88-450D-87E3-49D51BF23B91,215.00,购物,支出,2022年03月30日 16:14,1648628048160,"花呗（雨伞,防晒衣,卸妆油）"
            4A2228BC-0A82-4FE8-8E2A-26E82042C601,9000.00,薪资,收入,2022年04月01日 10:08,1648778888123,四月工资
        """.trimIndent()

        val envelope = BackupImportPolicy.parseExternalCsv(csv)
        val expense = envelope.payload.records.first { it.type == "EXPENSE" }
        val incomeCategory = envelope.payload.categories.first { it.name == "薪资" }

        assertEquals(2, envelope.payload.records.size)
        assertEquals("58009EF6-3A88-450D-87E3-49D51BF23B91", expense.syncId)
        assertEquals(1648628048L, expense.createdAt)
        assertEquals(1648628048L, expense.updatedAt)
        assertEquals("花呗（雨伞,防晒衣,卸妆油）", expense.note)
        assertEquals("salary", incomeCategory.semanticLabel)
        assertEquals("payments", incomeCategory.icon)
    }

    @Test
    fun parseExternalCsv_importsProvidedLargeCsvWhenConfigured() {
        val samplePath = System.getenv("MEWBOOK_SAMPLE_IMPORT_CSV").orEmpty()
        assumeTrue("Set MEWBOOK_SAMPLE_IMPORT_CSV to run this local fixture test", samplePath.isNotBlank())
        val sampleFile = File(samplePath)
        assumeTrue("Sample CSV does not exist: $samplePath", sampleFile.exists())

        val csvText = sampleFile.readText()
        val expectedDataRows = csvText.lineSequence()
            .drop(1)
            .count { it.isNotBlank() }
        val envelope = BackupImportPolicy.parseExternalCsv(csvText)

        assertEquals(expectedDataRows, envelope.payload.records.size)
        assertTrue(envelope.payload.categories.any { it.name == "学习" })
        assertTrue(envelope.payload.records.any { it.syncId == "0EEC2148-C284-4C5D-B3ED-A39C4F55D2BB" })
    }

    @Test
    fun mergeRecordImport_mapsSynonymCategoriesToExistingLocalCategories() {
        val current = semanticCurrentEnvelope(
            categories = listOf(
                BackupCategory(1, "餐饮", "restaurant", 0xFFFF6B6B, "EXPENSE", true, 0),
                BackupCategory(2, "早餐", "free_breakfast", 0xFFFF9F43, "EXPENSE", true, 0, parentId = 1),
                BackupCategory(3, "工资", "payments", 0xFF4CAF50, "INCOME", true, 0)
            )
        )
        val incoming = semanticIncomingEnvelope(
            categories = listOf(
                BackupCategory(10, "早饭", "restaurant", 0xFFFF6B6B, "EXPENSE", false, 0),
                BackupCategory(11, "薪资", "payments", 0xFF4CAF50, "INCOME", false, 0)
            ),
            records = listOf(
                semanticRecord(id = 1, categoryId = 10, type = "EXPENSE", note = "豆浆"),
                semanticRecord(id = 2, categoryId = 11, type = "INCOME", amount = 5000.0, note = "四月")
            )
        )

        val preview = BackupImportPolicy.previewRecordImport(current, incoming)
        val merged = BackupImportPolicy.mergeRecordImport(current, incoming)

        assertEquals(0, preview.categoriesToCreate)
        assertTrue(preview.categoryMappings.any { it.sourceName == "早饭" && it.targetName == "早餐" && it.action == BackupCategoryImportAction.REUSE_EXISTING })
        assertTrue(preview.categoryMappings.any { it.sourceName == "薪资" && it.targetName == "工资" && it.action == BackupCategoryImportAction.REUSE_EXISTING })
        assertEquals(3, merged.payload.categories.size)
        assertEquals(2, merged.payload.records.first { it.note == "豆浆" }.categoryId)
        assertEquals(3, merged.payload.records.first { it.note == "四月" }.categoryId)
    }

    @Test
    fun mergeRecordImport_createsUnknownSemanticCategoryWithUsefulIcon() {
        val current = semanticCurrentEnvelope(categories = emptyList())
        val incoming = semanticIncomingEnvelope(
            categories = listOf(
                BackupCategory(
                    id = 10,
                    name = "房租",
                    icon = "category",
                    color = 0xFFFF6B6B,
                    type = "EXPENSE",
                    isDefault = false,
                    sortOrder = 0,
                    semanticLabel = "housing"
                )
            ),
            records = listOf(semanticRecord(id = 1, categoryId = 10, type = "EXPENSE"))
        )

        val preview = BackupImportPolicy.previewRecordImport(current, incoming)
        val merged = BackupImportPolicy.mergeRecordImport(current, incoming)
        val createdCategory = merged.payload.categories.single()

        assertEquals(1, preview.categoriesToCreate)
        assertTrue(preview.categoryMappings.any { it.sourceName == "房租" && it.action == BackupCategoryImportAction.CREATE_NEW && it.icon == "home" })
        assertEquals("房租", createdCategory.name)
        assertEquals("home", createdCategory.icon)
    }

    @Test
    fun mergeRecordImport_doesNotGuessWhenSemanticMatchIsAmbiguous() {
        val current = semanticCurrentEnvelope(
            categories = listOf(
                BackupCategory(1, "早餐", "free_breakfast", 0xFFFF9F43, "EXPENSE", true, 0),
                BackupCategory(2, "早饭", "free_breakfast", 0xFFFF9F43, "EXPENSE", false, 1)
            )
        )
        val incoming = semanticIncomingEnvelope(
            categories = listOf(
                BackupCategory(10, "早点", "free_breakfast", 0xFFFF9F43, "EXPENSE", false, 0)
            ),
            records = listOf(semanticRecord(id = 1, categoryId = 10, type = "EXPENSE"))
        )

        val preview = BackupImportPolicy.previewRecordImport(current, incoming)
        val merged = BackupImportPolicy.mergeRecordImport(current, incoming)

        assertEquals(1, preview.categoriesToCreate)
        assertTrue(preview.categoryMappings.any { it.sourceName == "早点" && it.action == BackupCategoryImportAction.CREATE_NEW })
        assertTrue(merged.payload.categories.any { it.name == "早点" })
    }

    @Test
    fun mergeRecordImport_mapsSampleAppCategorySynonymsToLocalDefaults() {
        val current = semanticCurrentEnvelope(
            categories = listOf(
                BackupCategory(1, "日用", "cleaning_services", 0xFFA1887F, "EXPENSE", true, 0),
                BackupCategory(2, "居住", "home", 0xFF95E1D3, "EXPENSE", true, 1),
                BackupCategory(3, "投资收益", "trending_up", 0xFFB5EAD7, "INCOME", true, 0)
            )
        )
        val incoming = BackupImportPolicy.parseExternalCsv(
            """
                UUID,金额,分类,类型,日期,时间戳（毫秒）,备注
                1,20.34,日用品,支出,2022年04月05日 17:13,1649150023638,"卫生巾,牙膏"
                2,1800.00,住房,支出,2022年04月06日 09:00,1649206800000,房租
                3,88.00,基金收益,收入,2022年04月07日 09:00,1649293200000,基金分红
            """.trimIndent()
        )

        val preview = BackupImportPolicy.previewRecordImport(current, incoming)
        val merged = BackupImportPolicy.mergeRecordImport(current, incoming)

        assertEquals(0, preview.categoriesToCreate)
        assertTrue(preview.categoryMappings.any { it.sourceName == "日用品" && it.targetName == "日用" })
        assertTrue(preview.categoryMappings.any { it.sourceName == "住房" && it.targetName == "居住" })
        assertTrue(preview.categoryMappings.any { it.sourceName == "基金收益" && it.targetName == "投资收益" })
        assertEquals(3, merged.payload.categories.size)
    }

    private fun semanticCurrentEnvelope(categories: List<BackupCategory>): BackupEnvelope {
        return BackupEnvelope(
            schemaVersion = BackupMigration.CURRENT_SCHEMA_VERSION,
            appVersion = "test",
            exportedAt = "2026-04-25T00:00:00",
            payload = BackupPayload(
                categories = categories,
                ledgers = listOf(
                    BackupLedger(1, "我的账本", "PERSONAL", "person", 0xFF4CAF50, 1, true)
                )
            )
        )
    }

    private fun semanticIncomingEnvelope(
        categories: List<BackupCategory>,
        records: List<BackupRecord>
    ): BackupEnvelope {
        return BackupEnvelope(
            schemaVersion = BackupMigration.CURRENT_SCHEMA_VERSION,
            appVersion = "smart-import-test",
            exportedAt = "2026-04-25T00:00:00",
            payload = BackupPayload(
                categories = categories,
                records = records,
                ledgers = listOf(
                    BackupLedger(1, "我的账本", "PERSONAL", "person", 0xFF4CAF50, 1, true)
                )
            )
        )
    }

    private fun semanticRecord(
        id: Long,
        categoryId: Long,
        type: String,
        amount: Double = 12.0,
        note: String? = null
    ): BackupRecord {
        return BackupRecord(
            id = id,
            amount = amount,
            type = type,
            categoryId = categoryId,
            note = note,
            date = LocalDate.of(2026, 4, 25).toEpochDay(),
            createdAt = id,
            updatedAt = id,
            syncId = "sync-$id",
            ledgerId = 1,
            accountId = null
        )
    }
}
