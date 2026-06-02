package com.mewbook.app.domain.policy

import com.mewbook.app.domain.model.DeletedRecord

enum class RecycleBinAmountFilter {
    ALL,
    UP_TO_50,
    FROM_50_TO_200,
    OVER_200
}

object RecycleBinFilterPolicy {

    fun filter(
        records: List<DeletedRecord>,
        categoryId: Long?,
        amountFilter: RecycleBinAmountFilter
    ): List<DeletedRecord> {
        return records.filter { deletedRecord ->
            val record = deletedRecord.record
            val matchesCategory = categoryId == null || record.categoryId == categoryId
            val matchesAmount = when (amountFilter) {
                RecycleBinAmountFilter.ALL -> true
                RecycleBinAmountFilter.UP_TO_50 -> record.amount <= 50.0
                RecycleBinAmountFilter.FROM_50_TO_200 -> record.amount > 50.0 && record.amount <= 200.0
                RecycleBinAmountFilter.OVER_200 -> record.amount > 200.0
            }
            matchesCategory && matchesAmount
        }
    }
}
