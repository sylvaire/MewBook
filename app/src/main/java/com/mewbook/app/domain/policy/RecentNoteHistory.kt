package com.mewbook.app.domain.policy

import com.mewbook.app.domain.model.Record

object RecentNoteHistory {

    fun notesByCategory(records: List<Record>, ledgerId: Long): Map<Long, List<String>> {
        return records
            .asSequence()
            .filter { it.ledgerId == ledgerId }
            .mapNotNull { record ->
                val note = record.note?.trim().orEmpty()
                if (note.isBlank()) {
                    null
                } else {
                    record.categoryId to (note to record.updatedAt)
                }
            }
            .groupBy(
                keySelector = { it.first },
                valueTransform = { it.second }
            )
            .mapValues { (_, notes) ->
                notes
                    .groupBy(
                        keySelector = { it.first },
                        valueTransform = { it.second }
                    )
                    .mapValues { (_, timestamps) -> timestamps.max() }
                    .entries
                    .sortedByDescending { it.value }
                    .map { it.key }
            }
    }
}
