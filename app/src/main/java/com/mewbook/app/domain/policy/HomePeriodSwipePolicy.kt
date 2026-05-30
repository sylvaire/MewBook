package com.mewbook.app.domain.policy

enum class HomePeriodSwipeAction {
    Previous,
    Next,
    None
}

object HomePeriodSwipePolicy {

    fun resolve(
        totalDragX: Float,
        thresholdPx: Float,
        canGoNext: Boolean
    ): HomePeriodSwipeAction {
        return when {
            totalDragX > thresholdPx -> HomePeriodSwipeAction.Previous
            totalDragX < -thresholdPx && canGoNext -> HomePeriodSwipeAction.Next
            else -> HomePeriodSwipeAction.None
        }
    }
}
