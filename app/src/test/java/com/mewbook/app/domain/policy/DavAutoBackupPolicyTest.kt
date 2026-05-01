package com.mewbook.app.domain.policy

import com.mewbook.app.domain.model.DavConfig
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.LocalDate

class DavAutoBackupPolicyTest {

    @Test
    fun shouldRun_skipsWhenConfigMissingDisabledOrAlreadyAttemptedToday() {
        val today = LocalDate.of(2026, 4, 30)

        assertFalse(DavAutoBackupPolicy.shouldRun(null, null, today))
        assertFalse(
            DavAutoBackupPolicy.shouldRun(
                DavConfig(
                    serverUrl = "https://dav.example.com",
                    username = "demo",
                    password = "pass",
                    isEnabled = false
                ),
                null,
                today
            )
        )
        assertFalse(
            DavAutoBackupPolicy.shouldRun(
                DavConfig(
                    serverUrl = "https://dav.example.com",
                    username = "demo",
                    isEnabled = true
                ),
                today,
                today
            )
        )
    }

    @Test
    fun shouldRun_allowsConfiguredEnabledDavOnANewLocalDate() {
        val today = LocalDate.of(2026, 4, 30)

        val result = DavAutoBackupPolicy.shouldRun(
            config = DavConfig(
                serverUrl = "https://dav.example.com",
                username = "demo",
                password = "pass",
                isEnabled = true
            ),
            lastAttemptDate = today.minusDays(1),
            today = today
        )

        assertTrue(result)
    }

    @Test
    fun backupFilesToDelete_keepsLatestThirtyAutomaticJsonBackupsOnly() {
        val automaticBackupFiles = (1..35).map { day ->
            "https://dav.example.com/MewBook/mewbook_auto_backup_202604%02d_120000.json".format(day)
        }
        val files = automaticBackupFiles + listOf(
            "https://dav.example.com/MewBook/readme.txt",
            "https://dav.example.com/MewBook/custom_manual_export.json",
            "https://dav.example.com/MewBook/mewbook_backup_20260401_120000.json",
            "https://dav.example.com/MewBook/mewbook_auto_backup_20260401_120000.csv"
        )

        val result = DavAutoBackupPolicy.backupFilesToDelete(files, keepLatestCount = 30)

        assertEquals(5, result.size)
        assertEquals(automaticBackupFiles.take(5), result)
    }
}
