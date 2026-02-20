package com.merak.ui.page

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.merak.core.installer.ThemeFlags
import com.merak.core.installer.ThemeInstaller
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
    private val installer: ThemeInstaller
) : ViewModel() {

    private val _installEvent = Channel<InstallEvent>(Channel.BUFFERED)
    val installEvent = _installEvent.receiveAsFlow()

    private val _isDownloading = MutableStateFlow(false)
    val isDownloading = _isDownloading.asStateFlow()

    /**
     * 本地文件安装
     */
    fun installLocalTheme(context: Context, file: File, flags: Long = ThemeFlags.ALL) {
        viewModelScope.launch {
            _installEvent.send(InstallEvent.ShowToast(context.getString(R.string.copying_theme)))

            Timber.d("Applying local theme with combined flags: 0x${flags.toString(16).uppercase()}")

            val result = withContext(Dispatchers.IO) {
                val copyResult = installer.installThemeFromPath(file.absolutePath)

                copyResult.fold(
                    onSuccess = {
                        val applied = installer.applyTheme(flags)
                        if (applied) Result.success(true) else Result.failure(Exception("启动主题管理器失败"))
                    },
                    onFailure = { Result.failure(it) }
                )
            }

            handleInstallResult(context, result, "Local file: ${file.name} (Flags: 0x${flags.toString(16)})")
        }
    }

    /**
     * 在线 URL 安装
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
                    installer.installThemeFromUrl(url)
                }

                result.fold(
                    onSuccess = { file ->
                        // [修改] 使用 LogFormatter 记录规范日志 (标题 | 内容)
                        LogFormatter.logThemeInstall(
                            title = "在线主题安装成功",
                            content = "URL: $url\n文件: ${file.name} (${file.length()} bytes)"
                        )

                        withContext(Dispatchers.Main) {
                            installer.applyTheme()
                        }

                        _installEvent.send(InstallEvent.ShowToast(context.getString(R.string.download_complete)))
                        _installEvent.send(InstallEvent.NavigateBack)
                    },
                    onFailure = { error ->
                        Timber.e(error, "Online theme install failed")
                        _installEvent.send(InstallEvent.ShowToast("${context.getString(R.string.download_failed)}: ${error.message}"))
                    }
                )

            } catch (e: Exception) {
                Timber.e(e, "Online install exception")
                _installEvent.send(InstallEvent.ShowToast("Error: ${e.message}"))
            } finally {
                _isDownloading.value = false
            }
        }
    }

    /**
     * 统一处理安装结果
     */
    private suspend fun <T> handleInstallResult(context: Context, result: Result<T>, sourceInfo: String) {
        result.fold(
            onSuccess = {
                // [修改] 使用 LogFormatter 记录规范日志
                // 这将输出: D/THEME_INSTALL: 主题安装成功 | Source: Local file: ...
                // 这样 LogPage 就能正确解析出标题 "主题安装成功" 和内容 "Source: ..."
                // 并且 KeepAliveService 也能统计到 "/THEME_INSTALL:"
                LogFormatter.logThemeInstall(
                    title = "主题安装成功",
                    content = "来源: $sourceInfo"
                )

                _installEvent.send(InstallEvent.ShowToast(context.getString(R.string.theme_copied)))
                _installEvent.send(InstallEvent.NavigateBack)
            },
            onFailure = { error ->
                Timber.e(error, "Theme install failed. Source: $sourceInfo")
                _installEvent.send(InstallEvent.ShowToast(context.getString(R.string.copy_failed) + ": " + error.message))
            }
        )
    }
}

sealed interface InstallEvent {
    data class ShowToast(val message: String) : InstallEvent
    data object NavigateBack : InstallEvent
}