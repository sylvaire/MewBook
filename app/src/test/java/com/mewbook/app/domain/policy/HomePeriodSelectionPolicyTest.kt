package com.mewbook.app.domain.policy

import com.mewbook.app.domain.model.BudgetPeriodType
import org.junit.Assert.assertEquals
import org.junit.Test

class HomePeriodSelectionPolicyTest {

    @Test
    fun fromStorageValue_restoresSavedPeriodType() {
        assertEquals(
            BudgetPeriodType.WEEK,
            HomePeriodSelectionPolicy.fromStorageValue("WEEK")
        )
    }

    @Test
    fun fromStorageValue_fallsBackToMonthForUnknownValue() {
        assertEquals(
            BudgetPeriodType.MONTH,
            HomePeriodSelectionPolicy.fromStorageValue("YEAR")
        )
    }
}
