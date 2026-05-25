package com.merak.util.timber

import timber.log.Timber

/**
 * Unified log formatting utility.
 * Format: [Tag] Title | Content
 */
object LogFormatter {

    /** Tag for theme installation related logs. */
    const val TAG_THEME_INSTALL = "THEME_INSTALL"

    /** Tag for alarm or broadcast interception logs. */
    const val TAG_ALARM_INTERCEPT = "ALARM_INTERCEPT"

    /** General error tag. */
    const val TAG_ERROR = "ERROR"

    /** Critical crash tag. */
    const val TAG_CRASH = "CRASH"

    /**
     * Records theme installation logs.
     *
     * @param title The brief summary of the installation event.
     * @param content Additional details regarding the installation.
     */
    fun logThemeInstall(title: String, content: String = "") {
        // Output format: I/THEME_INSTALL: Title | Content
        Timber.tag(TAG_THEME_INSTALL).i("$title | $content")
    }

    /**
     * Records broadcast or alarm interception logs.
     *
     * @param title The action or component being intercepted.
     * @param content Specific reasons or metadata for the interception.
     */
    fun logAlarmIntercept(title: String, content: String = "") {
        Timber.tag(TAG_ALARM_INTERCEPT).i("$title | $content")
    }

    /**
     * Records critical application crash logs.
     *
     * @param e The throwable representing the crash.
     */
    fun logCrash(e: Throwable) {
        // Timber handles stack traces automatically when a Throwable is passed.
        Timber.tag(TAG_CRASH).e(e, "App Crash | ${e.message}")
    }

    /**
     * Records general error logs with optional exception details.
     *
     * @param title Descriptive message about the error.
     * @param e Optional [Throwable] for stack trace logging.
     */
    fun logError(title: String, e: Throwable? = null) {
        if (e != null) {
            Timber.tag(TAG_ERROR).e(e, "$title | ${e.message}")
        } else {
            Timber.tag(TAG_ERROR).e(title)
        }
    }
}