package com.mewbook.app.domain.policy

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class HomeScreenLayoutPolicyTest {

    @Test
    fun consumeBackPress_returnsTrueOnlyForSearchModeWithoutEditor() {
        assertTrue(
            HomeScreenLayoutPolicy.consumeBackPress(
                isSearchMode = true,
                isAddEditSheetVisible = false
            )
        )
        assertFalse(
            HomeScreenLayoutPolicy.consumeBackPress(
                isSearchMode = false,
                isAddEditSheetVisible = false
            )
        )
        assertFalse(
            HomeScreenLayoutPolicy.consumeBackPress(
                isSearchMode = true,
                isAddEditSheetVisible = true
            )
        )
    }

    @Test
    fun showSummaryAsScrollableHeader_onlyForNormalHomeWithRecords() {
        assertTrue(
            HomeScreenLayoutPolicy.showSummaryAsScrollableHeader(
                isSearchMode = false,
                hasRecords = true
            )
        )
        assertFalse(
            HomeScreenLayoutPolicy.showSummaryAsScrollableHeader(
                isSearchMode = true,
                hasRecords = true
            )
        )
        assertFalse(
            HomeScreenLayoutPolicy.showSummaryAsScrollableHeader(
                isSearchMode = false,
                hasRecords = false
            )
        )
    }
}
