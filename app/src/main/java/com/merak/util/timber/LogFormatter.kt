package com.merak.util.timber

import timber.log.Timber

/**
 * 统一日志格式化工具
 * 格式: [Tag] Title | Content
 */
object LogFormatter {

    // 统计用的 Tag 标识
    const val TAG_THEME_INSTALL = "THEME_INSTALL"
    const val TAG_ALARM_INTERCEPT = "ALARM_INTERCEPT"
    const val TAG_CRASH = "CRASH"

    /**
     * 记录主题安装日志
     */
    fun logThemeInstall(title: String, content: String = "") {
        // 输出格式: D/THEME_INSTALL: Title | Content
        Timber.tag(TAG_THEME_INSTALL).i("$title | $content")
    }

    /**
     * 记录广播拦截日志
     */
    fun logAlarmIntercept(title: String, content: String = "") {
        Timber.tag(TAG_ALARM_INTERCEPT).i("$title | $content")
    }

    /**
     * 记录崩溃日志
     */
    fun logCrash(e: Throwable) {
        // 崩溃日志比较特殊，Timber 会自动处理堆栈
        Timber.tag(TAG_CRASH).e(e, "App Crash | ${e.message}")
    }
}