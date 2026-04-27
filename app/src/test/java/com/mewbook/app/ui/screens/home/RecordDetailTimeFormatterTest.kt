package com.mewbook.app.ui.screens.home

import org.junit.Assert.assertEquals
import org.junit.Test
import java.time.LocalDate
import java.time.LocalDateTime

class RecordDetailTimeFormatterTest {

    @Test
    fun formatRecordDetailDateTime_combinesRecordDateWithSpecificTime() {
        assertEquals(
            "2026年4月25日 星期六 13:14:15",
            RecordDetailTimeFormatter.format(
                date = LocalDate.of(2026, 4, 25),
                timeSource = LocalDateTime.of(2026, 4, 26, 13, 14, 15)
            )
        )
    }
}
