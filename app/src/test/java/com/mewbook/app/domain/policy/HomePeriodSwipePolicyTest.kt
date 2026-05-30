package com.mewbook.app.domain.policy

import org.junit.Assert.assertEquals
import org.junit.Test

class HomePeriodSwipePolicyTest {

    @Test
    fun resolve_returnsPreviousWhenDraggingRightPastThreshold() {
        assertEquals(
            HomePeriodSwipeAction.Previous,
            HomePeriodSwipePolicy.resolve(
                totalDragX = 73f,
                thresholdPx = 72f,
                canGoNext = true
            )
        )
    }

    @Test
    fun resolve_returnsNextWhenDraggingLeftPastThresholdAndNextIsAllowed() {
        assertEquals(
            HomePeriodSwipeAction.Next,
            HomePeriodSwipePolicy.resolve(
                totalDragX = -73f,
                thresholdPx = 72f,
                canGoNext = true
            )
        )
    }

    @Test
    fun resolve_ignoresLeftDragWhenNextIsNotAllowed() {
        assertEquals(
            HomePeriodSwipeAction.None,
            HomePeriodSwipePolicy.resolve(
                totalDragX = -73f,
                thresholdPx = 72f,
                canGoNext = false
            )
        )
    }

    @Test
    fun resolve_ignoresSmallDrag() {
        assertEquals(
            HomePeriodSwipeAction.None,
            HomePeriodSwipePolicy.resolve(
                totalDragX = 24f,
                thresholdPx = 72f,
                canGoNext = true
            )
        )
    }
}
