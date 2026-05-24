package com.mewbook.app.domain.policy

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class AppUpdateCheckPolicyTest {
    @Test
    fun canStartCheck_allowsManualCheckWhenAutomaticUpdatesAreDisabled() {
        assertTrue(
            AppUpdateCheckPolicy.canStartCheck(
                isChecking = false,
                isDownloading = false,
                updateEnabled = false,
                silent = false
            )
        )
    }

    @Test
    fun canStartCheck_blocksSilentCheckWhenAutomaticUpdatesAreDisabled() {
        assertFalse(
            AppUpdateCheckPolicy.canStartCheck(
                isChecking = false,
                isDownloading = false,
                updateEnabled = false,
                silent = true
            )
        )
    }

    @Test
    fun canStartCheck_blocksWhenUpdateWorkIsAlreadyRunning() {
        assertFalse(
            AppUpdateCheckPolicy.canStartCheck(
                isChecking = true,
                isDownloading = false,
                updateEnabled = true,
                silent = false
            )
        )
        assertFalse(
            AppUpdateCheckPolicy.canStartCheck(
                isChecking = false,
                isDownloading = true,
                updateEnabled = false,
                silent = false
            )
        )
    }
}
