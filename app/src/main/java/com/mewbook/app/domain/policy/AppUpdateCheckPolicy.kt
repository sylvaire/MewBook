package com.mewbook.app.domain.policy

object AppUpdateCheckPolicy {
    fun canStartCheck(
        isChecking: Boolean,
        isDownloading: Boolean,
        updateEnabled: Boolean,
        silent: Boolean
    ): Boolean {
        if (isChecking || isDownloading) {
            return false
        }
        return updateEnabled || !silent
    }
}
