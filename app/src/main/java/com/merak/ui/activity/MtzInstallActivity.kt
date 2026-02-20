package com.merak.ui.activity

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.provider.OpenableColumns
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.lifecycle.lifecycleScope
import com.merak.core.installer.ThemeInstaller
import com.merak.core.os.shizuku.AutoAccessibilityManager
import com.merak.ui.components.MtzInstallDialog
import com.merak.ui.theme.ThemeStoreMiuixTheme
import com.merak.util.toast
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.koin.android.ext.android.inject
import org.koin.androidx.compose.koinViewModel
import org.koin.core.component.KoinComponent

class MtzInstallActivity : ComponentActivity(), KoinComponent {
    private val autoAccessibilityManager: AutoAccessibilityManager by inject()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        window.isNavigationBarContrastEnforced = false

        // Extract Uri based on the intent action
        val uri: Uri? = when (intent.action) {
            Intent.ACTION_VIEW -> intent.data
            Intent.ACTION_SEND -> intent.getParcelableExtra(Intent.EXTRA_STREAM, Uri::class.java)
            else -> null
        }

        // Validate the extracted Uri
        if (uri == null || uri.scheme != "content") {
            this.toast("无效的文件或不支持的协议")
            finish()
            return
        }

        val fileName = getFileNameFromUri(uri) ?: "unknown.mtz"

        // Check file extension
        if (!fileName.endsWith(".mtz", ignoreCase = true)) {
            this.toast("仅支持安装 MTZ 文件")
            finish()
            return
        }

        // Render the transparent UI with the dialog component
        setContent {
            val mainViewModel: MainViewModel = koinViewModel()
            val uiState by mainViewModel.uiState.collectAsState()

            if (uiState.isLoaded) {
                // Apply the user's customized theme settings to the dialog
                ThemeStoreMiuixTheme(
                    themeMode = uiState.themeMode,
                    useDynamicColor = uiState.useDynamicColor,
                    useMiuixMonet = uiState.useMiuixMonet,
                    seedColor = uiState.seedColor
                ) {
                    Box(modifier = Modifier.fillMaxSize()) {
                        MtzInstallDialog(
                            useWindowBlur = uiState.useBlur,
                            fileName = fileName,
                            onDismissRequest = {
                                finish()
                            },
                            onInstallConfirm = { flags ->
                                // Execute the installation flow
                                processAndInstall(uri, flags)
                            }
                        )
                    }
                }
            }
        }
    }

    /**
     * Reuses the logic in ThemeInstaller to pipe the stream directly to the target directory.
     */
    private fun processAndInstall(uri: Uri, flags: Long) {
        lifecycleScope.launch {
            val themeInstaller = ThemeInstaller(this@MtzInstallActivity)

            // Execute the IO operation on the IO dispatcher
            val copyResult = withContext(Dispatchers.IO) {
                themeInstaller.installThemeFromUri(this@MtzInstallActivity, uri)
            }

            copyResult.fold(
                onSuccess = {
                    // Trigger the theme manager to apply the copied file
                    val applied = themeInstaller.applyTheme(flags)
                    if (applied) {
                        this@MtzInstallActivity.toast("应用请求已发送")
                        autoAccessibilityManager.runCheck()
                    } else {
                        this@MtzInstallActivity.toast("启动主题管理器失败")
                    }
                },
                onFailure = { error ->
                    this@MtzInstallActivity.toast("文件处理失败: ${error.message}")
                }
            )

            // Close the transparent activity when done
            finish()
        }
    }

    /**
     * Resolves the display name of the file from the ContentProvider.
     */
    private fun getFileNameFromUri(uri: Uri): String? {
        var result: String? = null
        try {
            contentResolver.query(uri, null, null, null, null)?.use { cursor ->
                if (cursor.moveToFirst()) {
                    val nameIndex = cursor.getColumnIndex(OpenableColumns.DISPLAY_NAME)
                    if (nameIndex != -1) {
                        result = cursor.getString(nameIndex)
                    }
                }
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }

        // Fallback to the last segment of the path if the query fails
        return result ?: uri.lastPathSegment
    }
}