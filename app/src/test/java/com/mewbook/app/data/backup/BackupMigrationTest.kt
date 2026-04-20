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
