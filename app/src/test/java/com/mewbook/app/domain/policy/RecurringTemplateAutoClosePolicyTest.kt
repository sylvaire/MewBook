package com.mewbook.app.domain.policy

import com.mewbook.app.domain.model.RecordType
import com.mewbook.app.domain.model.RecurringTemplate
import com.mewbook.app.domain.model.RecurringTemplateScheduleType
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.LocalDate
import java.time.LocalDateTime

class RecurringTemplateAutoClosePolicyTest {

    @Test
    fun buildPlan_generatesAllDueOccurrencesUntilFutureDate() {
        val template = recurringTemplate(
            nextDueDate = LocalDate.of(2026, 4, 1),
            scheduleType = RecurringTemplateScheduleType.WEEKLY,
            intervalCount = 1,
            endDate = null
        )

        val plan = RecurringTemplateAutoClosePolicy.buildPlan(
            template = template,
            referenceDate = LocalDate.of(2026, 4, 22)
        )

        assertEquals(
            listOf(
                LocalDate.of(2026, 4, 1),
                LocalDate.of(2026, 4, 8),
                LocalDate.of(2026, 4, 15),
                LocalDate.of(2026, 4, 22)
            ),
            plan.occurrenceDates
        )
        assertEquals(LocalDate.of(2026, 4, 29), plan.nextDueDate)
        assertTrue(plan.shouldRemainEnabled)
    }

    @Test
    fun buildPlan_disablesTemplateAfterLastAllowedOccurrence() {
        val template = recurringTemplate(
            nextDueDate = LocalDate.of(2026, 1, 1),
            scheduleType = RecurringTemplateScheduleType.MONTHLY,
            intervalCount = 1,
            endDate = LocalDate.of(2026, 3, 1)
        )

        val plan = RecurringTemplateAutoClosePolicy.buildPlan(
            template = template,
            referenceDate = LocalDate.of(2026, 4, 20)
        )

        assertEquals(
            listOf(
                LocalDate.of(2026, 1, 1),
                LocalDate.of(2026, 2, 1),
                LocalDate.of(2026, 3, 1)
            ),
            plan.occurrenceDates
        )
        assertEquals(LocalDate.of(2026, 4, 1), plan.nextDueDate)
        assertFalse(plan.shouldRemainEnabled)
    }

    @Test
    fun buildPlan_returnsEmptyForDisabledOrFutureTemplate() {
        val disabledTemplate = recurringTemplate(
            nextDueDate = LocalDate.of(2026, 4, 1),
            scheduleType = RecurringTemplateScheduleType.WEEKLY,
            intervalCount = 1,
            endDate = null,
            isEnabled = false
        )
        val futureTemplate = recurringTemplate(
            nextDueDate = LocalDate.of(2026, 5, 1),
            scheduleType = RecurringTemplateScheduleType.MONTHLY,
            intervalCount = 1,
            endDate = null
        )

        assertTrue(
            RecurringTemplateAutoClosePolicy.buildPlan(
                template = disabledTemplate,
                referenceDate = LocalDate.of(2026, 4, 20)
            ).occurrenceDates.isEmpty()
        )
        assertTrue(
            RecurringTemplateAutoClosePolicy.buildPlan(
                template = futureTemplate,
                referenceDate = LocalDate.of(2026, 4, 20)
            ).occurrenceDates.isEmpty()
        )
    }

    private fun recurringTemplate(
        nextDueDate: LocalDate,
        scheduleType: RecurringTemplateScheduleType,
        intervalCount: Int,
        endDate: LocalDate?,
        isEnabled: Boolean = true
    ) = RecurringTemplate(
        id = 1L,
        name = "房租",
        amount = 3000.0,
        type = RecordType.EXPENSE,
        categoryId = 10L,
        noteTemplate = "月租",
        ledgerId = 1L,
        accountId = 100L,
        scheduleType = scheduleType,
        intervalCount = intervalCount,
        startDate = nextDueDate,
        nextDueDate = nextDueDate,
        endDate = endDate,
        isEnabled = isEnabled,
        reminderEnabled = false,
        lastGeneratedDate = null,
        createdAt = LocalDateTime.of(2026, 1, 1, 8, 0),
        updatedAt = LocalDateTime.of(2026, 1, 1, 8, 0)
    )
}
