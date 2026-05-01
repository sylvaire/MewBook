package com.mewbook.app.domain.policy

import com.mewbook.app.domain.model.RecurringTemplate
import com.mewbook.app.domain.model.RecurringTemplateScheduleType
import java.time.LocalDate

object RecurringTemplateSchedulePolicy {

    fun advanceNextDueDate(
        currentDueDate: LocalDate,
        scheduleType: RecurringTemplateScheduleType,
        intervalCount: Int
    ): LocalDate {
        val interval = intervalCount.coerceAtLeast(1).toLong()
        return when (scheduleType) {
            RecurringTemplateScheduleType.WEEKLY -> currentDueDate.plusWeeks(interval)
            RecurringTemplateScheduleType.MONTHLY -> currentDueDate.plusMonths(interval)
            RecurringTemplateScheduleType.CUSTOM_DAYS -> currentDueDate.plusDays(interval)
        }
    }

    fun describeSchedule(
        scheduleType: RecurringTemplateScheduleType,
        intervalCount: Int
    ): String {
        val interval = intervalCount.coerceAtLeast(1)
        return when (scheduleType) {
            RecurringTemplateScheduleType.WEEKLY -> "每 $interval 周"
            RecurringTemplateScheduleType.MONTHLY -> "每 $interval 月"
            RecurringTemplateScheduleType.CUSTOM_DAYS -> "每 $interval 天"
        }
    }

    fun displayName(scheduleType: RecurringTemplateScheduleType): String {
        return when (scheduleType) {
            RecurringTemplateScheduleType.WEEKLY -> "按周"
            RecurringTemplateScheduleType.MONTHLY -> "按月"
            RecurringTemplateScheduleType.CUSTOM_DAYS -> "自定义天数"
        }
    }

    fun isDue(template: RecurringTemplate, referenceDate: LocalDate = LocalDate.now()): Boolean {
        if (!template.isEnabled) {
            return false
        }
        if (template.nextDueDate > referenceDate) {
            return false
        }
        val endDate = template.endDate
        return endDate == null || template.nextDueDate <= endDate
    }

    fun canProcessCurrentOccurrence(template: RecurringTemplate): Boolean {
        if (!template.isEnabled) {
            return false
        }
        val endDate = template.endDate
        return endDate == null || template.nextDueDate <= endDate
    }
}
