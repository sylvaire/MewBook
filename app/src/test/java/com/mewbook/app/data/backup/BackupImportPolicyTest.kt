package com.mewbook.app.data.backup

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

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
}
