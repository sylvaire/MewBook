package com.mewbook.app.domain.model

data class Budget(
    val id: Long = 0,
    val categoryId: Long?,   // null表示总预算
    val periodType: BudgetPeriodType = BudgetPeriodType.MONTH,
    val periodKey: String,
    val amount: Double,
    val ledgerId: Long
) {
    val month: String get() = periodKey
}

data class BudgetWithSpending(
    val budget: Budget,
    val spent: Double
) {
    val remaining: Double get() = budget.amount - spent
    val progress: Float get() = if (budget.amount > 0) (spent / budget.amount).toFloat().coerceIn(0f, 1f) else 0f
    val isOverBudget: Boolean get() = spent > budget.amount
    val isWarning: Boolean get() = progress >= 0.7f && !isOverBudget
    val isDanger: Boolean get() = progress >= 0.9f
}
