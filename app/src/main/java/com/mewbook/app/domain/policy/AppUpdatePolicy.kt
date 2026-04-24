package com.mewbook.app.domain.policy

import com.mewbook.app.domain.model.AppUpdateAsset
import com.mewbook.app.domain.model.AppUpdateRelease

object AppUpdatePolicy {

    fun selectInstallableApkAsset(assets: List<AppUpdateAsset>): AppUpdateAsset? {
        return assets
            .filter { asset -> asset.name.endsWith(".apk", ignoreCase = true) }
            .maxWithOrNull(
                compareBy<AppUpdateAsset> { asset -> apkAssetScore(asset.name) }
                    .thenBy { asset -> asset.sizeBytes }
            )
    }

    fun isNewerRelease(releaseVersionName: String, currentVersionName: String): Boolean {
        val releaseParts = parseVersionParts(releaseVersionName) ?: return false
        val currentParts = parseVersionParts(currentVersionName) ?: return false
        val maxSize = maxOf(releaseParts.size, currentParts.size)
        for (index in 0 until maxSize) {
            val releasePart = releaseParts.getOrElse(index) { 0 }
            val currentPart = currentParts.getOrElse(index) { 0 }
            if (releasePart != currentPart) {
                return releasePart > currentPart
            }
        }
        return false
    }

    fun toAvailableReleaseOrNull(
        tagName: String,
        releaseName: String?,
        htmlUrl: String?,
        notes: String?,
        assets: List<AppUpdateAsset>,
        currentVersionName: String
    ): AppUpdateRelease? {
        if (!isNewerRelease(tagName, currentVersionName)) {
            return null
        }
        val apkAsset = selectInstallableApkAsset(assets) ?: return null
        return AppUpdateRelease(
            versionName = cleanVersionName(tagName),
            releaseName = releaseName?.takeIf { it.isNotBlank() } ?: tagName,
            htmlUrl = htmlUrl.orEmpty(),
            notes = notes.orEmpty(),
            asset = apkAsset
        )
    }

    fun cleanVersionName(value: String): String {
        return parseVersionRegex.find(value)?.value ?: value.trim()
    }

    private fun parseVersionParts(value: String): List<Int>? {
        return parseVersionRegex.find(value)
            ?.value
            ?.split(".")
            ?.mapNotNull { part -> part.toIntOrNull() }
            ?.takeIf { it.isNotEmpty() }
    }

    private fun apkAssetScore(name: String): Int {
        val normalized = name.lowercase()
        return listOf(
            "release" to 4,
            "mewbook" to 3,
            "universal" to 2,
            "arm64" to 1
        ).sumOf { (keyword, score) ->
            if (normalized.contains(keyword)) score else 0
        }
    }

    private val parseVersionRegex = Regex("""\d+(?:\.\d+)+""")
}
