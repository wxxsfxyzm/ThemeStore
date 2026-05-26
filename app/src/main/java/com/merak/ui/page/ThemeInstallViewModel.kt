package com.merak.ui.page

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.merak.core.installer.ThemeFlags
import com.merak.core.installer.ThemeInstallerManager // Updated import
import com.merak.util.timber.LogFormatter
import com.merak.x.R
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import timber.log.Timber
import java.io.File

class ThemeInstallViewModel(
    private val installerManager: ThemeInstallerManager
) : ViewModel() {

    private val _installEvent = Channel<InstallEvent>(Channel.BUFFERED)
    val installEvent = _installEvent.receiveAsFlow()

    private val _isDownloading = MutableStateFlow(false)
    val isDownloading = _isDownloading.asStateFlow()

    /**
     * Local file installation
     */
    fun installLocalTheme(context: Context, file: File, flags: Long = ThemeFlags.ALL) {
        viewModelScope.launch {
            _installEvent.send(InstallEvent.ShowToast(context.getString(R.string.copying_theme)))

            Timber.d("Applying local theme with combined flags: 0x${flags.toString(16).uppercase()}")

            val result = withContext(Dispatchers.IO) {
                // Call through the manager, no context needed for the method
                val copyResult = installerManager.installThemeFromPath(file.absolutePath)

                copyResult.fold(
                    onSuccess = {
                        val applied = installerManager.applyTheme(flags)
                        if (applied) {
                            Result.success(true)
                        } else {
                            Result.failure(Exception("Failed to start theme manager"))
                        }
                    },
                    onFailure = { Result.failure(it) }
                )
            }

            handleInstallResult(
                context = context,
                result = result,
                sourceInfo = context.getString(
                    R.string.theme_install_local_source,
                    file.name,
                    flags.toString(16).uppercase()
                )
            )
        }
    }

    /**
     * Online URL installation
     */
    fun installOnlineTheme(context: Context, url: String) {
        if (url.isBlank()) {
            viewModelScope.launch {
                _installEvent.send(InstallEvent.ShowToast(context.getString(R.string.input_url_hint)))
            }
            return
        }

        viewModelScope.launch {
            _isDownloading.value = true
            _installEvent.send(InstallEvent.ShowToast(context.getString(R.string.download_started)))

            try {
                val result = withContext(Dispatchers.IO) {
                    // Call through the manager
                    installerManager.installThemeFromUrl(url)
                }

                result.fold(
                    onSuccess = { file ->
                        LogFormatter.logThemeInstall(
                            title = context.getString(R.string.theme_install_success),
                            content = context.getString(
                                R.string.theme_install_online_source,
                                url,
                                file.name,
                                file.length()
                            )
                        )

                        withContext(Dispatchers.Main) {
                            installerManager.applyTheme()
                        }

                        _installEvent.send(InstallEvent.ShowToast(context.getString(R.string.download_complete)))
                        _installEvent.send(InstallEvent.NavigateBack)
                    },
                    onFailure = { error ->
                        Timber.e(error, "Online theme install failed")
                        LogFormatter.logThemeInstall(
                            title = context.getString(R.string.theme_install_failed),
                            content = context.getString(
                                R.string.theme_install_online_failure,
                                url,
                                error.message ?: error::class.java.simpleName
                            )
                        )
                        _installEvent.send(InstallEvent.ShowToast("${context.getString(R.string.download_failed)}: ${error.message}"))
                    }
                )

            } catch (e: Exception) {
                Timber.e(e, "Online install exception")
                LogFormatter.logThemeInstall(
                    title = context.getString(R.string.theme_install_failed),
                    content = context.getString(
                        R.string.theme_install_online_failure,
                        url,
                        e.message ?: e::class.java.simpleName
                    )
                )
                _installEvent.send(InstallEvent.ShowToast("Error: ${e.message}"))
            } finally {
                _isDownloading.value = false
            }
        }
    }

    /**
     * Unified installation result handler
     */
    private suspend fun <T> handleInstallResult(context: Context, result: Result<T>, sourceInfo: String) {
        result.fold(
            onSuccess = {
                LogFormatter.logThemeInstall(
                    title = context.getString(R.string.theme_install_success),
                    content = context.getString(R.string.theme_install_source, sourceInfo)
                )

                _installEvent.send(InstallEvent.ShowToast(context.getString(R.string.theme_copied)))
                _installEvent.send(InstallEvent.NavigateBack)
            },
            onFailure = { error ->
                Timber.e(error, "Theme install failed")
                LogFormatter.logThemeInstall(
                    title = context.getString(R.string.theme_install_failed),
                    content = when (error.message) {
                        "Failed to start theme manager" -> context.getString(R.string.theme_install_failed_starting_manager)
                        "Source file does not exist" -> context.getString(R.string.theme_install_source_file_not_exist)
                        "Source file cannot be read" -> context.getString(R.string.theme_install_source_file_not_readable)
                        "File copy failed" -> context.getString(R.string.theme_install_file_copy_failed)
                        "File download failed" -> context.getString(R.string.theme_install_file_download_failed)
                        else -> context.getString(
                            R.string.theme_install_error_detail,
                            error.message ?: error::class.java.simpleName
                        )
                    }
                )
                _installEvent.send(InstallEvent.ShowToast(context.getString(R.string.copy_failed) + ": " + error.message))
            }
        )
    }
}

sealed interface InstallEvent {
    data class ShowToast(val message: String) : InstallEvent
    data object NavigateBack : InstallEvent
}
