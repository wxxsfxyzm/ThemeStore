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
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.lifecycle.lifecycleScope
import com.merak.core.installer.ThemeInstallerManager
import com.merak.service.ThemeInstallAccessibilityService
import com.merak.ui.components.MtzInstallDialog
import com.merak.ui.theme.ThemeStoreMiuixTheme
import com.merak.util.timber.LogFormatter
import com.merak.util.toast
import com.merak.x.R
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.koin.android.ext.android.inject
import org.koin.androidx.compose.koinViewModel
import org.koin.core.component.KoinComponent
import timber.log.Timber

class MtzInstallActivity : ComponentActivity(), KoinComponent {

    private val themeInstallerManager: ThemeInstallerManager by inject()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        // Extract Uri based on the intent action
        val uri: Uri? = when (intent.action) {
            Intent.ACTION_VIEW -> intent.data
            Intent.ACTION_SEND -> intent.getParcelableExtra(Intent.EXTRA_STREAM, Uri::class.java)
            else -> null
        }

        // Validate the extracted Uri
        if (uri == null || uri.scheme != "content") {
            this.toast(R.string.theme_install_invalid_file_or_uri_scheme)
            finish()
            return
        }

        val fileName = uri.getFileName() ?: "unknown.mtz"

        // Check file extension
        if (!fileName.endsWith(".mtz", ignoreCase = true)) {
            this.toast(R.string.theme_install_only_support_mtz)
            finish()
            return
        }

        // Render the transparent UI with the dialog component
        setContent {
            val mainViewModel: MainViewModel = koinViewModel()
            val uiState by mainViewModel.uiState.collectAsState()

            if (uiState.isLoaded) {
                if (uiState.showWelcome) {
                    // Intercept and redirect to MainActivity if OOBE is not completed
                    LaunchedEffect(Unit) {
                        this@MtzInstallActivity.toast("请先完成应用初始设置")

                        val mainIntent = Intent(this@MtzInstallActivity, MainActivity::class.java).apply {
                            // Clear task to ensure MainActivity starts fresh and becomes the root
                            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK)
                        }
                        startActivity(mainIntent)
                        finish()
                    }
                } else {
                    // Apply the user's customized theme settings to the dialog
                    ThemeStoreMiuixTheme(
                        themeMode = uiState.themeMode,
                        useDynamicColor = uiState.useDynamicColor,
                        useMiuixMonet = uiState.useMiuixMonet,
                        seedColor = uiState.seedColor,
                        paletteStyle = uiState.paletteStyle,
                        colorSpec = uiState.colorSpec
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
    }

    /**
     * Reuses the logic in ThemeInstallerManager to pipe the stream directly to the target directory.
     */
    private fun processAndInstall(uri: Uri, flags: Long) {
        lifecycleScope.launch {
            // Execute the IO operation on the IO dispatcher using the injected manager
            val copyResult = withContext(Dispatchers.IO) {
                themeInstallerManager.installThemeFromUri(uri)
            }

            copyResult.fold(
                onSuccess = {
                    // Trigger the theme manager to apply the copied file
                    val applied = themeInstallerManager.applyTheme(flags)
                    if (applied) {
                        this@MtzInstallActivity.toast(R.string.theme_install_request_sent)
                        LogFormatter.logThemeInstall(
                            title = this@MtzInstallActivity.getString(R.string.theme_install_success),
                            content = this@MtzInstallActivity.getString(
                                R.string.theme_install_source_with_flags,
                                uri.getFileName(),
                                flags.toString(16).uppercase()
                            )
                        )

                        // Request notification refresh
                        ThemeInstallAccessibilityService.requestRefresh()
                        CoroutineScope(Dispatchers.IO).launch {
                            delay(500L) // Wait for log file IO to complete
                            ThemeInstallAccessibilityService.requestRefresh()
                        }
                    } else {
                        this@MtzInstallActivity.toast(R.string.theme_install_failed_starting_manager)
                        Timber.e("Theme manager did not accept the apply request")
                        LogFormatter.logThemeInstall(
                            title = this@MtzInstallActivity.getString(R.string.theme_install_failed),
                            content = this@MtzInstallActivity.getString(R.string.theme_install_failed_starting_manager)
                        )
                    }
                },
                onFailure = { error ->
                    this@MtzInstallActivity.toast(this@MtzInstallActivity.getString(R.string.theme_install_failed_handle_file, error.message))
                    Timber.e(error, "Theme install failed while handling URI")
                    LogFormatter.logThemeInstall(
                        title = this@MtzInstallActivity.getString(R.string.theme_install_failed),
                        content = this@MtzInstallActivity.getString(
                            R.string.theme_install_failed_handle_file,
                            uri.toString()
                        )
                    )
                }
            )

            // Close the transparent activity when done
            finish()
        }
    }

    /**
     * Resolves the display name of the file from the ContentProvider.
     */
    private fun Uri.getFileName(): String? {
        var result: String? = null
        try {
            contentResolver.query(this, null, null, null, null)?.use { cursor ->
                if (cursor.moveToFirst()) {
                    val nameIndex = cursor.getColumnIndex(OpenableColumns.DISPLAY_NAME)
                    if (nameIndex != -1) {
                        result = cursor.getString(nameIndex)
                    }
                }
            }
        } catch (e: Exception) {
            Timber.e(e, "Failed to resolve display name from URI")
        }

        // Fallback to the last segment of the path if the query fails
        return result ?: this.lastPathSegment
    }
}
