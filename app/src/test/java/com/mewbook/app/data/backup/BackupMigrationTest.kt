package com.mewbook.app.data.backup

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class BackupMigrationTest {

    @Test
    fun parseLegacyExportV1_migratesRecordsAndCategories() {
        val legacyJson = """
            {
              "exportTime": "2026-04-18T12:00:00",
              "version": "1.0",
              "records": [
                {
                  "date": "2026-04-18",
                  "type": "EXPENSE",
                  "categoryName": "餐饮",
                  "subCategoryName": "早餐",
                  "amount": 18.5,
                  "note": "豆浆油条"
                }
              ]
            }
        """.trimIndent()

        val envelope = BackupMigration.parseToCurrentEnvelope(legacyJson)

        assertEquals(BackupMigration.CURRENT_SCHEMA_VERSION, envelope.schemaVersion)
        assertEquals(1, envelope.payload.records.size)
        assertEquals(2, envelope.payload.categories.size)
        assertEquals("legacy-export-v1", envelope.appVersion)
        assertEquals(1L, envelope.payload.records.first().ledgerId)
    }

    @Test
    fun parseLegacyDavV2_preservesIdsAndLedgerReferences() {
        val legacyJson = """
            {
              "version": 2,
              "exportTime": "2026-04-18T12:00:00",
              "records": [
                {
                  "id": 9,
                  "amount": 100.0,
                  "type": "EXPENSE",
                  "categoryId": 2,
                  "note": "早餐",
                  "date": 19831,
                  "createdAt": 1713400000,
                  "updatedAt": 1713400100,
                  "syncId": "sync-1",
                  "ledgerId": 3,
                  "accountId": null
                }
              ],
              "categories": [
                {
                  "id": 2,
                  "name": "早餐",
                  "icon": "restaurant",
                  "color": 4290000000,
                  "type": "EXPENSE",
                  "isDefault": false,
                  "sortOrder": 0,
                  "parentId": null
                }
              ]
            }
        """.trimIndent()

        val envelope = BackupMigration.parseToCurrentEnvelope(legacyJson)

        assertEquals(9L, envelope.payload.records.first().id)
        assertEquals(3L, envelope.payload.records.first().ledgerId)
        assertEquals(2L, envelope.payload.categories.first().id)
        assertTrue(envelope.payload.ledgers.any { it.id == 3L })
    }

    @Test
    fun parseCurrentEnvelope_keepsPayload() {
        val currentJson = """
            {
              "schemaVersion": 3,
              "appVersion": "1.0.1",
              "exportedAt": "2026-04-18T12:00:00",
              "payload": {
                "records": [],
                "categories": [],
                "accounts": [],
                "budgets": [],
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
                ],
                "davConfig": null,
                "themeMode": "system"
              }
            }
        """.trimIndent()

        val envelope = BackupMigration.parseToCurrentEnvelope(currentJson)

        assertEquals("1.0.1", envelope.appVersion)
        assertEquals("system", envelope.payload.themeMode)
        assertEquals(1, envelope.payload.ledgers.size)
        assertNull(envelope.payload.davConfig)
    }

    @Test
    fun parseSchema3Envelope_backfillsBudgetPeriodFields() {
        val currentJson = """
            {
              "schemaVersion": 3,
              "appVersion": "1.0.2",
              "exportedAt": "2026-04-18T12:00:00",
              "payload": {
                "records": [],
                "categories": [],
                "accounts": [],
                "budgets": [
                  {
                    "id": 7,
                    "categoryId": null,
                    "month": "2026-04",
                    "amount": 800.0,
                    "ledgerId": 1
                  }
                ],
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

        val envelope = BackupMigration.parseToCurrentEnvelope(currentJson)
        val budget = envelope.payload.budgets.first()

        assertEquals("MONTH", budget.periodType)
        assertEquals("2026-04", budget.periodKey)
    }

    @Test
    fun summarizeEnvelope_countsAllSectionsAndFlags() {
        val json = """
            {
              "schemaVersion": 4,
              "appVersion": "1.0.3",
              "exportedAt": "2026-04-18T12:00:00",
              "payload": {
                "records": [
                  {
                    "id": 1,
                    "amount": 20.0,
                    "type": "EXPENSE",
                    "categoryId": 2,
                    "note": "早餐",
                    "date": 19831,
                    "createdAt": 1713400000,
                    "updatedAt": 1713400100,
                    "syncId": "sync-1",
                    "ledgerId": 1,
                    "accountId": 3
                  }
                ],
                "categories": [
                  {
                    "id": 2,
                    "name": "早餐",
                    "icon": "restaurant",
                    "color": 4290000000,
                    "type": "EXPENSE",
                    "isDefault": false,
                    "sortOrder": 0,
                    "parentId": null
                  }
                ],
                "accounts": [
                  {
                    "id": 3,
                    "name": "现金",
                    "type": "CASH",
                    "balance": 100.0,
                    "icon": "wallet",
                    "color": 4283215696,
                    "isDefault": true,
                    "sortOrder": 0,
                    "ledgerId": 1
                  }
                ],
                "budgets": [
                  {
                    "id": 4,
                    "categoryId": null,
                    "periodType": "MONTH",
                    "periodKey": "2026-04",
                    "month": "2026-04",
                    "amount": 800.0,
                    "ledgerId": 1
                  }
                ],
                "templates": [
                  {
                    "id": 6,
                    "name": "房租",
                    "amount": 2500.0,
                    "type": "EXPENSE",
                    "categoryId": 2,
                    "noteTemplate": "每月房租",
                    "ledgerId": 1,
                    "accountId": 3,
                    "scheduleType": "MONTHLY",
                    "intervalCount": 1,
                    "startDate": 19800,
                    "nextDueDate": 19831,
                    "endDate": null,
                    "isEnabled": true,
                    "reminderEnabled": false,
                    "lastGeneratedDate": null,
                    "createdAt": 1713400200,
                    "updatedAt": 1713400200
                  }
                ],
                "ledgers": [
                  {
                    "id": 5,
                    "name": "我的账本",
                    "type": "PERSONAL",
                    "icon": "person",
                    "color": 4283215696,
                    "createdAt": 1,
                    "isDefault": true
                  }
                ],
                "davConfig": {
                  "serverUrl": "https://example.com",
                  "username": "user",
                  "password": "pass",
                  "remotePath": "/MewBook",
                  "isEnabled": true,
                  "lastSyncTime": 1713400000000
                },
                "themeMode": "dark"
              }
            }
        """.trimIndent()

        val envelope = BackupMigration.parseToCurrentEnvelope(json)
        val summary = BackupMigration.summarizeEnvelope(envelope)

        assertEquals(1, summary.records)
        assertEquals(1, summary.categories)
        assertEquals(1, summary.accounts)
        assertEquals(1, summary.budgets)
        assertEquals(1, summary.templates)
        assertEquals(1, summary.ledgers)
        assertTrue(summary.hasDavConfig)
        assertEquals("dark", summary.themeMode)
    }

    @Test
    fun compareEnvelopes_reportsConflictsByEntityId() {
        val currentJson = """
            {
              "schemaVersion": 4,
              "appVersion": "1.0.3",
              "exportedAt": "2026-04-18T12:00:00",
              "payload": {
                "records": [
                  {
                    "id": 1,
                    "amount": 20.0,
                    "type": "EXPENSE",
                    "categoryId": 2,
                    "note": "早餐",
                    "date": 19831,
                    "createdAt": 1713400000,
                    "updatedAt": 1713400100,
                    "syncId": "sync-1",
                    "ledgerId": 1,
                    "accountId": 3
                  }
                ],
                "categories": [
                  {
                    "id": 2,
                    "name": "早餐",
                    "icon": "restaurant",
                    "color": 4290000000,
                    "type": "EXPENSE",
                    "isDefault": false,
                    "sortOrder": 0,
                    "parentId": null
                  }
                ],
                "accounts": [
                  {
                    "id": 3,
                    "name": "现金",
                    "type": "CASH",
                    "balance": 100.0,
                    "icon": "wallet",
                    "color": 4283215696,
                    "isDefault": true,
                    "sortOrder": 0,
                    "ledgerId": 1
                  }
                ],
                "budgets": [
                  {
                    "id": 4,
                    "categoryId": null,
                    "periodType": "MONTH",
                    "periodKey": "2026-04",
                    "month": "2026-04",
                    "amount": 800.0,
                    "ledgerId": 1
                  }
                ],
                "templates": [
                  {
                    "id": 6,
                    "name": "房租",
                    "amount": 2500.0,
                    "type": "EXPENSE",
                    "categoryId": 2,
                    "noteTemplate": "每月房租",
                    "ledgerId": 1,
                    "accountId": 3,
                    "scheduleType": "MONTHLY",
                    "intervalCount": 1,
                    "startDate": 19800,
                    "nextDueDate": 19831,
                    "endDate": null,
                    "isEnabled": true,
                    "reminderEnabled": false,
                    "lastGeneratedDate": null,
                    "createdAt": 1713400200,
                    "updatedAt": 1713400200
                  }
                ],
                "ledgers": [
                  {
                    "id": 5,
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

        val incomingJson = """
            {
              "schemaVersion": 4,
              "appVersion": "1.0.3",
              "exportedAt": "2026-04-19T12:00:00",
              "payload": {
                "records": [
                  {
                    "id": 1,
                    "amount": 25.0,
                    "type": "EXPENSE",
                    "categoryId": 2,
                    "note": "午餐",
                    "date": 19832,
                    "createdAt": 1713500000,
                    "updatedAt": 1713500100,
                    "syncId": "sync-2",
                    "ledgerId": 1,
                    "accountId": 3
                  }
                ],
                "categories": [
                  {
                    "id": 2,
                    "name": "午餐",
                    "icon": "restaurant",
                    "color": 4290000000,
                    "type": "EXPENSE",
                    "isDefault": false,
                    "sortOrder": 0,
                    "parentId": null
                  }
                ],
                "accounts": [
                  {
                    "id": 3,
                    "name": "银行卡",
                    "type": "BANK",
                    "balance": 200.0,
                    "icon": "wallet",
                    "color": 4283215696,
                    "isDefault": false,
                    "sortOrder": 0,
                    "ledgerId": 1
                  }
                ],
                "budgets": [
                  {
                    "id": 4,
                    "categoryId": null,
                    "periodType": "MONTH",
                    "periodKey": "2026-04",
                    "month": "2026-04",
                    "amount": 1000.0,
                    "ledgerId": 1
                  }
                ],
                "templates": [
                  {
                    "id": 6,
                    "name": "房租",
                    "amount": 2500.0,
                    "type": "EXPENSE",
                    "categoryId": 2,
                    "noteTemplate": "每月房租",
                    "ledgerId": 1,
                    "accountId": 3,
                    "scheduleType": "MONTHLY",
                    "intervalCount": 1,
                    "startDate": 19800,
                    "nextDueDate": 19832,
                    "endDate": null,
                    "isEnabled": true,
                    "reminderEnabled": false,
                    "lastGeneratedDate": null,
                    "createdAt": 1713500200,
                    "updatedAt": 1713500200
                  }
                ],
                "ledgers": [
                  {
                    "id": 5,
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

        val current = BackupMigration.parseToCurrentEnvelope(currentJson)
        val incoming = BackupMigration.parseToCurrentEnvelope(incomingJson)
        val preview = BackupMigration.compareEnvelopes(current, incoming)

        assertEquals(1, preview.conflicts.records)
        assertEquals(1, preview.conflicts.categories)
        assertEquals(1, preview.conflicts.accounts)
        assertEquals(1, preview.conflicts.budgets)
        assertEquals(1, preview.conflicts.templates)
        assertEquals(1, preview.conflicts.ledgers)
        assertTrue(preview.hasExistingData)
    }

    @Test
    fun parseFutureEnvelope_throwsClearError() {
        val futureJson = """
            {
              "schemaVersion": 99,
              "appVersion": "9.9.9",
              "exportedAt": "2026-04-18T12:00:00",
              "payload": {
                "records": [],
                "categories": [],
                "accounts": [],
                "budgets": [],
                "ledgers": []
              }
            }
        """.trimIndent()

        val error = runCatching {
            BackupMigration.parseToCurrentEnvelope(futureJson)
        }.exceptionOrNull()

        requireNotNull(error)
        assertTrue(error is IllegalArgumentException)
        assertTrue(error.message?.contains("newer than supported") == true)
        assertTrue(error.message?.contains("v99") == true)
    }
}
