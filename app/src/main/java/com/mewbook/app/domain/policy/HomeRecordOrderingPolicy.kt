package com.mewbook.app.domain.policy

import com.mewbook.app.domain.model.Record

object HomeRecordOrderingPolicy {

    fun newestFirst(records: List<Record>): List<Record> {
        return records.sortedWith(
            compareByDescending<Record> { it.date }
                .thenByDescending { it.createdAt }
        )
    }
}
