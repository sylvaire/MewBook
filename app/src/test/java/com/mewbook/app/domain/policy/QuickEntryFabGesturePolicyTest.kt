package com.mewbook.app.domain.policy

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class QuickEntryFabGesturePolicyTest {

    @Test
    fun resolveTap_returnsSingleTapWhenThereIsNoPreviousTap() {
        assertEquals(
            QuickEntryFabGesturePolicy.Result.SingleTap,
            QuickEntryFabGesturePolicy.resolveTap(previousTapAtMillis = null, nowMillis = 1000L)
        )
    }

    @Test
    fun resolveTap_returnsDoubleTapInsideWindowIncludingBoundary() {
        assertEquals(
            QuickEntryFabGesturePolicy.Result.DoubleTap,
            QuickEntryFabGesturePolicy.resolveTap(previousTapAtMillis = 1000L, nowMillis = 1280L)
        )
    }

    @Test
    fun resolveTap_returnsSingleTapOutsideWindow() {
        assertEquals(
            QuickEntryFabGesturePolicy.Result.SingleTap,
            QuickEntryFabGesturePolicy.resolveTap(previousTapAtMillis = 1000L, nowMillis = 1281L)
        )
    }

    @Test
    fun longPress_isSeparateFromTapCounting() {
        assertEquals(
            QuickEntryFabGesturePolicy.Result.LongPress,
            QuickEntryFabGesturePolicy.resolveLongPress()
        )
    }

    @Test
    fun shouldRunPendingSingleTap_onlyAfterWindowExpires() {
        assertFalse(
            QuickEntryFabGesturePolicy.shouldRunPendingSingleTap(
                tapAtMillis = 1000L,
                nowMillis = 1279L
            )
        )
        assertTrue(
            QuickEntryFabGesturePolicy.shouldRunPendingSingleTap(
                tapAtMillis = 1000L,
                nowMillis = 1280L
            )
        )
    }
}
