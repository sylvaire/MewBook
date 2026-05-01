package com.mewbook.app

import android.content.ActivityNotFoundException
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.provider.Settings
import androidx.activity.ComponentActivity
import androidx.activity.enableEdgeToEdge
import androidx.activity.compose.setContent
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.viewModels
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.getValue
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.core.content.FileProvider
import com.mewbook.app.ui.navigation.MewBookNavHost
import com.mewbook.app.domain.policy.DavAutoBackupCoordinator
import com.mewbook.app.ui.theme.MewBookTheme
import com.mewbook.app.data.preferences.AppThemeMode
import com.mewbook.app.ui.theme.ThemeViewModel
import com.mewbook.app.ui.update.AppUpdateUiState
import com.mewbook.app.ui.update.AppUpdateViewModel
import dagger.hilt.android.AndroidEntryPoint
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.lifecycleScope
import kotlinx.coroutines.launch
import java.io.File
import javax.inject.Inject

@AndroidEntryPoint
class MainActivity : ComponentActivity() {
    private val themeViewModel: ThemeViewModel by viewModels()
    private val appUpdateViewModel: AppUpdateViewModel by viewModels()
    @Inject lateinit var davAutoBackupCoordinator: DavAutoBackupCoordinator
    private lateinit var installApkLauncher: ActivityResultLauncher<Intent>
    private var pendingInstallAfterPermissionPath: String? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        installApkLauncher = registerForActivityResult(ActivityResultContracts.StartActivityForResult()) {
            // The package installer owns the result UI; no extra in-app state is needed here.
        }
        enableEdgeToEdge()
        setContent {
            val systemDarkTheme = isSystemInDarkTheme()
            val themeMode by themeViewModel.themeMode.collectAsStateWithLifecycle()
            val updateUiState by appUpdateViewModel.uiState.collectAsStateWithLifecycle()
            val isDarkTheme = when (themeMode) {
                AppThemeMode.SYSTEM -> systemDarkTheme
                AppThemeMode.LIGHT -> false
                AppThemeMode.DARK -> true
            }

            MewBookTheme(
                darkTheme = isDarkTheme,
                systemBarColorOverride = null
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(MaterialTheme.colorScheme.background)
                ) {
                    Surface(
                        modifier = Modifier.fillMaxSize(),
                        color = MaterialTheme.colorScheme.background
                    ) {
                        MewBookNavHost(
                            updateUiState = updateUiState,
                            onCheckForUpdates = {
                                appUpdateViewModel.checkForUpdates(silent = false)
                            }
                        )
                        AppUpdateDialogs(
                            uiState = updateUiState,
                            currentVersionName = BuildConfig.VERSION_NAME,
                            onDismissUpdate = appUpdateViewModel::dismissUpdateDialog,
                            onDownloadUpdate = appUpdateViewModel::downloadAvailableUpdate,
                            onSnoozeVersion = appUpdateViewModel::snoozeCurrentVersion,
                            onDisableUpdate = appUpdateViewModel::disableUpdateChecking,
                            onDismissInstall = appUpdateViewModel::dismissInstallDialog,
                            onInstall = ::tryInstallDownloadedApk,
                            onDismissPermission = appUpdateViewModel::dismissInstallPermissionDialog,
                            onOpenInstallPermissionSettings = ::openInstallPermissionSettings,
                            onDismissMessage = appUpdateViewModel::dismissMessage
                        )
                    }
                }
            }
        }
    }

    override fun onStart() {
        super.onStart()
        lifecycleScope.launch {
            davAutoBackupCoordinator.runIfDue()
        }
    }

    override fun onResume() {
        super.onResume()
        val pendingPath = pendingInstallAfterPermissionPath ?: return
        if (canRequestPackageInstalls()) {
            pendingInstallAfterPermissionPath = null
            tryInstallDownloadedApk(pendingPath)
        }
    }

    private fun tryInstallDownloadedApk(apkPath: String?) {
        if (apkPath.isNullOrBlank()) {
            appUpdateViewModel.showInstallError("安装包路径无效")
            return
        }
        val apkFile = File(apkPath)
        if (!apkFile.exists()) {
            appUpdateViewModel.showInstallError("安装包不存在，请重新下载")
            return
        }
        if (!canRequestPackageInstalls()) {
            pendingInstallAfterPermissionPath = apkPath
            appUpdateViewModel.showInstallPermissionDialog()
            return
        }
        val apkUri = FileProvider.getUriForFile(
            this,
            "$packageName.fileprovider",
            apkFile
        )
        val installIntent = Intent(Intent.ACTION_VIEW).apply {
            setDataAndType(apkUri, "application/vnd.android.package-archive")
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
            putExtra(Intent.EXTRA_RETURN_RESULT, true)
        }
        runCatching {
            installApkLauncher.launch(installIntent)
        }.onFailure { throwable ->
            appUpdateViewModel.showInstallError(throwable.message ?: "无法打开系统安装器")
        }
    }

    private fun openInstallPermissionSettings(apkPath: String?) {
        pendingInstallAfterPermissionPath = apkPath
        appUpdateViewModel.dismissInstallPermissionDialog()
        val settingsIntent = Intent(
            Settings.ACTION_MANAGE_UNKNOWN_APP_SOURCES,
            Uri.parse("package:$packageName")
        )
        try {
            startActivity(settingsIntent)
        } catch (_: ActivityNotFoundException) {
            appUpdateViewModel.showInstallError("无法打开安装权限设置")
        }
    }

    private fun canRequestPackageInstalls(): Boolean {
        return Build.VERSION.SDK_INT < Build.VERSION_CODES.O || packageManager.canRequestPackageInstalls()
    }
}

@Composable
private fun AppUpdateDialogs(
    uiState: AppUpdateUiState,
    currentVersionName: String,
    onDismissUpdate: () -> Unit,
    onDownloadUpdate: () -> Unit,
    onSnoozeVersion: () -> Unit,
    onDisableUpdate: () -> Unit,
    onDismissInstall: () -> Unit,
    onInstall: (String?) -> Unit,
    onDismissPermission: () -> Unit,
    onOpenInstallPermissionSettings: (String?) -> Unit,
    onDismissMessage: () -> Unit
) {
    val release = uiState.availableRelease
    if (uiState.showUpdateDialog && release != null && !uiState.isDownloading) {
        AlertDialog(
            onDismissRequest = onDismissUpdate,
            title = { Text("发现新版本 ${release.versionName}") },
            text = {
                Column {
                    Text("当前版本：$currentVersionName")
                    Text("最新版本：${release.releaseName}")
                    val notes = releaseNotesPreview(release.notes)
                    if (notes.isNotBlank()) {
                        Text(notes)
                    }
                }
            },
            confirmButton = {
                TextButton(onClick = onDownloadUpdate) {
                    Text("下载更新")
                }
            },
            dismissButton = {
                Row {
                    TextButton(onClick = onSnoozeVersion) {
                        Text("跳过此版本")
                    }
                    TextButton(onClick = onDisableUpdate) {
                        Text("关闭更新")
                    }
                    TextButton(onClick = onDismissUpdate) {
                        Text("稍后")
                    }
                }
            }
        )
    }

    if (uiState.isDownloading) {
        AlertDialog(
            onDismissRequest = {},
            title = { Text("正在下载更新") },
            text = {
                Column {
                    Text("安装包正在后台下载，完成后会提示安装。")
                    androidx.compose.foundation.layout.Spacer(modifier = Modifier.height(12.dp))
                    LinearProgressIndicator(modifier = Modifier.fillMaxWidth())
                    uiState.downloadProgressPercent?.let { percent ->
                        androidx.compose.foundation.layout.Spacer(modifier = Modifier.height(8.dp))
                        Text("$percent%")
                    }
                }
            },
            confirmButton = {}
        )
    }

    if (uiState.showInstallDialog && uiState.downloadedApkPath != null) {
        AlertDialog(
            onDismissRequest = onDismissInstall,
            title = { Text("更新下载完成") },
            text = { Text("安装包已下载完成，可以开始安装新版本。") },
            confirmButton = {
                TextButton(onClick = { onInstall(uiState.downloadedApkPath) }) {
                    Text("安装")
                }
            },
            dismissButton = {
                TextButton(onClick = onDismissInstall) {
                    Text("稍后")
                }
            }
        )
    }

    if (uiState.showInstallPermissionDialog) {
        AlertDialog(
            onDismissRequest = onDismissPermission,
            title = { Text("需要安装权限") },
            text = { Text("请允许喵喵记账安装未知应用，授权后会继续打开安装界面。") },
            confirmButton = {
                TextButton(onClick = { onOpenInstallPermissionSettings(uiState.downloadedApkPath) }) {
                    Text("去设置")
                }
            },
            dismissButton = {
                TextButton(onClick = onDismissPermission) {
                    Text("稍后")
                }
            }
        )
    }

    val message = uiState.errorMessage ?: uiState.infoMessage
    if (message != null) {
        AlertDialog(
            onDismissRequest = onDismissMessage,
            title = { Text(if (uiState.errorMessage != null) "更新失败" else "检查更新") },
            text = { Text(message) },
            confirmButton = {
                TextButton(onClick = onDismissMessage) {
                    Text("知道了")
                }
            }
        )
    }
}

private fun releaseNotesPreview(notes: String): String {
    return notes
        .lineSequence()
        .map { it.trim() }
        .filter { it.isNotBlank() }
        .take(6)
        .joinToString(separator = "\n")
        .take(400)
}
