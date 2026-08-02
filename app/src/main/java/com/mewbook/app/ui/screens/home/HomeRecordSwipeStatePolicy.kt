package com.mewbook.app.ui.screens.home

internal object HomeRecordSwipeStatePolicy {
    fun open(currentId: Long?, requestedId: Long): Long =
        if (currentId == requestedId) currentId else requestedId

    fun close(currentId: Long?, requestedId: Long): Long? =
        currentId?.takeUnless { it == requestedId }

    fun retainVisible(currentId: Long?, visibleIds: Set<Long>): Long? =
        currentId?.takeIf(visibleIds::contains)
}
