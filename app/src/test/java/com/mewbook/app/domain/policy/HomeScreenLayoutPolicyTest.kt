package com.mewbook.app.domain.policy

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class HomeScreenLayoutPolicyTest {

    @Test
    fun resetSearchOnRouteChange_onlyWhenLeavingHomeRoute() {
        assertTrue(
            HomeScreenLayoutPolicy.resetSearchOnRouteChange(
                previousRoute = "home",
                currentRoute = "settings",
                homeRoute = "home"
            )
        )
        assertFalse(
            HomeScreenLayoutPolicy.resetSearchOnRouteChange(
                previousRoute = "home",
                currentRoute = "home",
                homeRoute = "home"
            )
        )
        assertFalse(
            HomeScreenLayoutPolicy.resetSearchOnRouteChange(
                previousRoute = "settings",
                currentRoute = "home",
                homeRoute = "home"
            )
        )
        assertFalse(
            HomeScreenLayoutPolicy.resetSearchOnRouteChange(
                previousRoute = null,
                currentRoute = "home",
                homeRoute = "home"
            )
        )
        assertFalse(
            HomeScreenLayoutPolicy.resetSearchOnRouteChange(
                previousRoute = "home",
                currentRoute = null,
                homeRoute = "home"
            )
        )
    }

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
    fun showHomeHeaderAsScrollableContent_onlyForNormalHomeWithRecords() {
        assertTrue(
            HomeScreenLayoutPolicy.showHomeHeaderAsScrollableContent(
                isSearchMode = false,
                hasRecords = true
            )
        )
        assertFalse(
            HomeScreenLayoutPolicy.showHomeHeaderAsScrollableContent(
                isSearchMode = true,
                hasRecords = true
            )
        )
        assertFalse(
            HomeScreenLayoutPolicy.showHomeHeaderAsScrollableContent(
                isSearchMode = false,
                hasRecords = false
            )
        )
    }
}
