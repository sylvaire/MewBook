package com.mewbook.app.domain.policy

import com.mewbook.app.domain.model.BudgetPeriodType

object HomePeriodSelectionPolicy {

    fun toStorageValue(periodType: BudgetPeriodType): String = periodType.name

    fun fromStorageValue(value: String?): BudgetPeriodType {
        return BudgetPeriodType.entries.firstOrNull { it.name == value } ?: BudgetPeriodType.MONTH
    }
}
