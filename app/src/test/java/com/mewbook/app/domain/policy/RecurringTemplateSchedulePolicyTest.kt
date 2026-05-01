package com.mewbook.app.domain.policy

import com.mewbook.app.domain.model.RecordType
import com.mewbook.app.domain.model.RecurringTemplate
import com.mewbook.app.domain.model.RecurringTemplateScheduleType
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.LocalDate
import java.time.LocalDateTime

class RecurringTemplateSchedulePolicyTest {

    @Test
    fun canProcessCurrentOccurrence_rejectsDisabledOrEndedTemplates() {
        assertFalse(
            RecurringTemplateSchedulePolicy.canProcessCurrentOccurrence(
                template = recurringTemplate(isEnabled = false)
            )
        )
        assertFalse(
            RecurringTemplateSchedulePolicy.canProcessCurrentOccurrence(
                template = recurringTemplate(
                    nextDueDate = LocalDate.of(2026, 5, 1),
                    endDate = LocalDate.of(2026, 4, 30)
                )
            )
        )
    }

    @Test
    fun canProcessCurrentOccurrence_allowsEnabledTemplatesBeforeEndDate() {
        assertTrue(
            RecurringTemplateSchedulePolicy.canProcessCurrentOccurrence(
                template = recurringTemplate(
                    nextDueDate = LocalDate.of(2026, 4, 30),
                    endDate = LocalDate.of(2026, 5, 31)
                )
            )
        )
    }

    private fun recurringTemplate(
        nextDueDate: LocalDate = LocalDate.of(2026, 4, 29),
        endDate: LocalDate? = null,
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
        scheduleType = RecurringTemplateScheduleType.MONTHLY,
        intervalCount = 1,
        startDate = LocalDate.of(2026, 1, 1),
        nextDueDate = nextDueDate,
        endDate = endDate,
        isEnabled = isEnabled,
        reminderEnabled = false,
        lastGeneratedDate = null,
        createdAt = LocalDateTime.of(2026, 1, 1, 8, 0),
        updatedAt = LocalDateTime.of(2026, 1, 1, 8, 0)
    )
}
