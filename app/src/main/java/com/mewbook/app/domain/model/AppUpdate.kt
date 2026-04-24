package com.mewbook.app.domain.model

data class AppUpdateAsset(
    val name: String,
    val downloadUrl: String,
    val sizeBytes: Long
)

data class AppUpdateRelease(
    val versionName: String,
    val releaseName: String,
    val htmlUrl: String,
    val notes: String,
    val asset: AppUpdateAsset
)
