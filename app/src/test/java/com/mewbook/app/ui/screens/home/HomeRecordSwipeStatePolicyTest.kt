package com.mewbook.app.ui.screens.home

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class HomeRecordSwipeStatePolicyTest {
    @Test
    fun `opening a record replaces the previously expanded record`() {
        assertEquals(22L, HomeRecordSwipeStatePolicy.open(currentId = 11L, requestedId = 22L))
    }

    @Test
    fun `closing the expanded record clears it`() {
        assertNull(HomeRecordSwipeStatePolicy.close(currentId = 11L, requestedId = 11L))
    }

    @Test
    fun `closing another record preserves the expanded record`() {
        assertEquals(11L, HomeRecordSwipeStatePolicy.close(currentId = 11L, requestedId = 22L))
    }

    @Test
    fun `missing expanded record is removed after list refresh`() {
        assertNull(HomeRecordSwipeStatePolicy.retainVisible(currentId = 11L, visibleIds = setOf(22L)))
    }

    @Test
    fun `empty list always clears expanded record`() {
        assertNull(HomeRecordSwipeStatePolicy.retainVisible(currentId = 11L, visibleIds = emptySet()))
    }
}
