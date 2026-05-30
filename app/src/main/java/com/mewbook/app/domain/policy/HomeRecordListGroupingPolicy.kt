package com.mewbook.app.domain.policy

import com.mewbook.app.domain.model.BudgetPeriodType
import com.mewbook.app.domain.model.Record
import java.time.LocalDate

sealed interface HomeRecordListEntry {
    data class DateHeader(val date: LocalDate) : HomeRecordListEntry
    data class RecordEntry(val record: Record) : HomeRecordListEntry
}

object HomeRecordListGroupingPolicy {

    fun buildEntries(
        records: List<Record>,
        periodType: BudgetPeriodType
    ): List<HomeRecordListEntry> {
        if (periodType == BudgetPeriodType.DAY) {
            return records.map(HomeRecordListEntry::RecordEntry)
        }

        val entries = mutableListOf<HomeRecordListEntry>()
        var previousDate: LocalDate? = null
        records.forEach { record ->
            if (record.date != previousDate) {
                entries += HomeRecordListEntry.DateHeader(record.date)
                previousDate = record.date
            }
            entries += HomeRecordListEntry.RecordEntry(record)
        }
        return entries
    }
}
