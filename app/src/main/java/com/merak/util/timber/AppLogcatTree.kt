package com.merak.util.timber

import timber.log.Timber

/**
 * Debug logcat tree for technical diagnostics.
 *
 * User-facing history entries are written by [FileLoggingTree] and may be
 * localized, so they are filtered out of logcat.
 */
class AppLogcatTree : Timber.DebugTree() {
    private val fileOnlyTags = setOf(
        LogFormatter.TAG_THEME_INSTALL,
        LogFormatter.TAG_ALARM_INTERCEPT
    )

    override fun log(priority: Int, tag: String?, message: String, t: Throwable?) {
        if (tag != null && tag in fileOnlyTags) return
        super.log(priority, tag, message, t)
    }
}
