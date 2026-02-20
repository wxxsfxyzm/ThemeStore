package com.merak.util.timber

import android.content.Context
import com.merak.data.settings.repo.SettingsRepo
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import timber.log.Timber

/**
 * 专门负责管理日志系统的开启与关闭
 */
class LogController(
    private val context: Context,
    private val settingsRepo: SettingsRepo
) {
    private var fileLoggingTree: FileLoggingTree? = null
    private val scope = CoroutineScope(Dispatchers.Main)

    fun init() {
        // 监听设置变化 (假设 AppSettings 里有个 isLogEnabled 字段)
        // 这里为了演示，我先假设你可能还没加这个字段，或者暂时默认开启
        // 如果 AppSettings 里有 enableFileLogging，就用流监听：

        /* scope.launch {
            settingsRepo.appSettings
                .map { it.enableFileLogging } // 假设你有这个字段
                .distinctUntilChanged()
                .collect { enabled ->
                    updateLoggingState(enabled)
                }
        }
        */

        // 暂时默认开启 (或者根据 BuildConfig.DEBUG)
        updateLoggingState(true)
    }

    private fun updateLoggingState(enabled: Boolean) {
        if (enabled) {
            if (fileLoggingTree == null) {
                val tree = FileLoggingTree(context)
                Timber.plant(tree)
                fileLoggingTree = tree
            }
        } else {
            fileLoggingTree?.let { tree ->
                Timber.uproot(tree)
                tree.release()
                fileLoggingTree = null
            }
        }
    }
}