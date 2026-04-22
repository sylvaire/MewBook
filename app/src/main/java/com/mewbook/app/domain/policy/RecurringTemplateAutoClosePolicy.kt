package com.mewbook.app.domain.policy

import com.mewbook.app.domain.model.RecurringTemplate
import java.time.LocalDate

data class RecurringTemplateAutoClosePlan(
    val occurrenceDates: List<LocalDate>,
    val nextDueDate: LocalDate,
    val shouldRemainEnabled: Boolean
)

object RecurringTemplateAutoClosePolicy {

    fun buildPlan(
        template: RecurringTemplate,
        referenceDate: LocalDate = LocalDate.now()
    ): RecurringTemplateAutoClosePlan {
        if (!template.isEnabled) {
            return RecurringTemplateAutoClosePlan(
                occurrenceDates = emptyList(),
                nextDueDate = template.nextDueDate,
                shouldRemainEnabled = false
            )
        }

        val endDate = template.endDate
        if (template.nextDueDate > referenceDate) {
            val stillEnabled = endDate?.let { template.nextDueDate <= it } ?: true
            return RecurringTemplateAutoClosePlan(
                occurrenceDates = emptyList(),
                nextDueDate = template.nextDueDate,
                shouldRemainEnabled = stillEnabled
            )
        }

        val occurrenceDates = mutableListOf<LocalDate>()
        var currentDueDate = template.nextDueDate
        var shouldRemainEnabled = template.isEnabled

        while (
            shouldRemainEnabled &&
            currentDueDate <= referenceDate &&
            (endDate == null || currentDueDate <= endDate)
        ) {
            occurrenceDates += currentDueDate
            currentDueDate = RecurringTemplateSchedulePolicy.advanceNextDueDate(
                currentDueDate = currentDueDate,
                scheduleType = template.scheduleType,
                intervalCount = template.intervalCount
            )
            shouldRemainEnabled = endDate?.let { currentDueDate <= it } ?: true
        }

        return RecurringTemplateAutoClosePlan(
            occurrenceDates = occurrenceDates,
            nextDueDate = currentDueDate,
            shouldRemainEnabled = shouldRemainEnabled
        )
    }
}
