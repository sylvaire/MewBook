package com.mewbook.app.data.smartimport

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.ZoneId

class SmartImportPolicyTest {

    @Test
    fun parseAiResponseToEnvelope_acceptsJsonObjectWrappedInMarkdownFence() {
        val response = """
            ```json
            {
              "records": [
                {
                  "date": "2026-04-25",
                  "type": "EXPENSE",
                  "amount": 18.5,
                  "category": "早饭",
                  "categorySemantic": "food",
                  "subCategory": "",
                  "account": "支付宝",
                  "ledger": "我的账本",
                  "note": "豆浆油条",
                  "icon": "free_breakfast"
                },
                {
                  "date": "2026-04-25",
                  "type": "INCOME",
                  "amount": 5000,
                  "category": "薪资",
                  "categorySemantic": "salary",
                  "account": "银行卡",
                  "note": "四月工资"
                }
              ]
            }
            ```
        """.trimIndent()

        val envelope = SmartImportPolicy.parseAiResponseToEnvelope(response)

        assertEquals(2, envelope.payload.records.size)
        assertEquals(2, envelope.payload.categories.size)
        assertEquals(2, envelope.payload.accounts.size)
        assertEquals(LocalDate.of(2026, 4, 25).toEpochDay(), envelope.payload.records.first().date)
        assertTrue(envelope.payload.categories.any { it.name == "早饭" && it.semanticLabel == "food" && it.icon == "free_breakfast" })
        assertTrue(envelope.payload.categories.any { it.name == "薪资" && it.semanticLabel == "salary" && it.type == "INCOME" })
    }

    @Test(expected = IllegalArgumentException::class)
    fun parseAiResponseToEnvelope_rejectsNegativeAmount() {
        SmartImportPolicy.parseAiResponseToEnvelope(
            """
                [
                  {
                    "date": "2026-04-25",
                    "type": "EXPENSE",
                    "amount": -18.5,
                    "category": "餐饮"
                  }
                ]
            """.trimIndent()
        )
    }

    @Test(expected = IllegalArgumentException::class)
    fun parseAiResponseToEnvelope_rejectsInvalidDate() {
        SmartImportPolicy.parseAiResponseToEnvelope(
            """
                [
                  {
                    "date": "四月二十五日",
                    "type": "EXPENSE",
                    "amount": 18.5,
                    "category": "餐饮"
                  }
                ]
            """.trimIndent()
        )
    }

    @Test
    fun parseAiResponseToEnvelope_defaultsMissingDateToCurrentTime() {
        val now = LocalDateTime.of(2026, 4, 25, 13, 14, 15)

        val envelope = SmartImportPolicy.parseAiResponseToEnvelope(
            """
                [
                  {
                    "type": "EXPENSE",
                    "amount": 18.5,
                    "category": "餐饮",
                    "note": "没有日期的文本记录"
                  }
                ]
            """.trimIndent(),
            now = now
        )

        val record = envelope.payload.records.single()
        assertEquals(LocalDate.of(2026, 4, 25).toEpochDay(), record.date)
        assertEquals(now.atZone(ZoneId.systemDefault()).toEpochSecond(), record.createdAt)
    }
}
