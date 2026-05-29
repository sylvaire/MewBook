package com.mewbook.app.domain.policy

object QuickEntryFabGesturePolicy {
    const val DOUBLE_TAP_WINDOW_MILLIS: Long = 280L

    enum class Result {
        SingleTap,
        DoubleTap,
        LongPress
    }

    fun resolveTap(
        previousTapAtMillis: Long?,
        nowMillis: Long,
        doubleTapWindowMillis: Long = DOUBLE_TAP_WINDOW_MILLIS
    ): Result {
        val previousTap = previousTapAtMillis ?: return Result.SingleTap
        val elapsed = nowMillis - previousTap
        return if (elapsed in 0..doubleTapWindowMillis) {
            Result.DoubleTap
        } else {
            Result.SingleTap
        }
    }

    fun resolveLongPress(): Result = Result.LongPress

    fun shouldRunPendingSingleTap(
        tapAtMillis: Long,
        nowMillis: Long,
        doubleTapWindowMillis: Long = DOUBLE_TAP_WINDOW_MILLIS
    ): Boolean = nowMillis - tapAtMillis >= doubleTapWindowMillis
}
