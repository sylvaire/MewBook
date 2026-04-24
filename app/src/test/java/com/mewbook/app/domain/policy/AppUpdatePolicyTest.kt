package com.mewbook.app.domain.policy

import com.mewbook.app.domain.model.AppUpdateAsset
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class AppUpdatePolicyTest {

    @Test
    fun newerRelease_acceptsVersionTagsWithPrefix() {
        assertTrue(AppUpdatePolicy.isNewerRelease("v1.0.5", "1.0.4"))
        assertTrue(AppUpdatePolicy.isNewerRelease("release-2.1.0", "2.0.9"))
    }

    @Test
    fun newerRelease_rejectsSameOrOlderVersions() {
        assertFalse(AppUpdatePolicy.isNewerRelease("v1.0.4", "1.0.4"))
        assertFalse(AppUpdatePolicy.isNewerRelease("v1.0.3", "1.0.4"))
        assertFalse(AppUpdatePolicy.isNewerRelease("not-a-version", "1.0.4"))
    }

    @Test
    fun selectInstallableApkAsset_prefersReleaseMewBookApk() {
        val asset = AppUpdatePolicy.selectInstallableApkAsset(
            listOf(
                AppUpdateAsset("notes.txt", "https://example.com/notes.txt", 1),
                AppUpdateAsset("app-debug.apk", "https://example.com/debug.apk", 10),
                AppUpdateAsset("MewBook-release.apk", "https://example.com/release.apk", 9)
            )
        )

        assertNotNull(asset)
        assertEquals("MewBook-release.apk", asset?.name)
    }

    @Test
    fun toAvailableRelease_requiresNewVersionAndApkAsset() {
        val assets = listOf(AppUpdateAsset("MewBook-release.apk", "https://example.com/release.apk", 9))

        assertNotNull(
            AppUpdatePolicy.toAvailableReleaseOrNull(
                tagName = "v1.0.5",
                releaseName = "MewBook 1.0.5",
                htmlUrl = "https://github.com/sylvaire/MewBook/releases/tag/v1.0.5",
                notes = "bug fixes",
                assets = assets,
                currentVersionName = "1.0.4"
            )
        )
        assertNull(
            AppUpdatePolicy.toAvailableReleaseOrNull(
                tagName = "v1.0.4",
                releaseName = "MewBook 1.0.4",
                htmlUrl = "",
                notes = "",
                assets = assets,
                currentVersionName = "1.0.4"
            )
        )
        assertNull(
            AppUpdatePolicy.toAvailableReleaseOrNull(
                tagName = "v1.0.5",
                releaseName = "MewBook 1.0.5",
                htmlUrl = "",
                notes = "",
                assets = emptyList(),
                currentVersionName = "1.0.4"
            )
        )
    }
}
