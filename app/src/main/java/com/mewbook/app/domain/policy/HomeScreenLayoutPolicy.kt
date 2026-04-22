package com.mewbook.app.domain.policy

object HomeScreenLayoutPolicy {

    fun resetSearchOnRouteChange(
        previousRoute: String?,
        currentRoute: String?,
        homeRoute: String
    ): Boolean {
        return previousRoute == homeRoute && currentRoute != null && currentRoute != homeRoute
    }

    fun consumeBackPress(
        isSearchMode: Boolean,
        isAddEditSheetVisible: Boolean
    ): Boolean {
        return isSearchMode && !isAddEditSheetVisible
    }

    fun showHomeHeaderAsScrollableContent(
        isSearchMode: Boolean,
        hasRecords: Boolean
    ): Boolean {
        return !isSearchMode && hasRecords
    }
}
