package com.mewbook.app.domain.policy

object HomeScreenLayoutPolicy {

    fun consumeBackPress(
        isSearchMode: Boolean,
        isAddEditSheetVisible: Boolean
    ): Boolean {
        return isSearchMode && !isAddEditSheetVisible
    }

    fun showSummaryAsScrollableHeader(
        isSearchMode: Boolean,
        hasRecords: Boolean
    ): Boolean {
        return !isSearchMode && hasRecords
    }
}
