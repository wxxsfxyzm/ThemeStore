package com.merak.util.timber

import android.content.Context
import android.util.Log
import com.merak.x.BuildConfig
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.channels.consumeEach
import kotlinx.coroutines.launch
import timber.log.Timber
import java.io.File
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@Suppress("LogNotTimber")
class FileLoggingTree(
    private val context: Context
) : Timber.DebugTree() {

    companion object {
        // Max size for a single log file: 4MB
        private const val MAX_FILE_SIZE = 4 * 1024 * 1024L

        // Max retention time: 7 days in milliseconds
        private const val MAX_RETAIN_DAYS_MS = 7L * 24 * 60 * 60 * 1000L
    }

    private val logDir: File by lazy { File(context.filesDir, "logs") }
    private val dateFormat = SimpleDateFormat("yyyy-MM-dd HH:mm:ss.SSS", Locale.US)
    private val fileNameFormat = SimpleDateFormat("yyyy-MM-dd", Locale.US)
    private val timeSuffixFormat = SimpleDateFormat("_HHmmss", Locale.US)

    // A set of allowed tags for Release builds
    private val allowedReleaseTags = setOf(
        LogFormatter.TAG_THEME_INSTALL,
        LogFormatter.TAG_ALARM_INTERCEPT,
        LogFormatter.TAG_CRASH
    )

    // Channel buffer for async logging
    private val logChannel = Channel<String>(Channel.UNLIMITED)
    private val scope = CoroutineScope(Dispatchers.IO + SupervisorJob())

    init {
        // Clean up expired logs asynchronously upon initialization
        scope.launch {
            cleanUpOldLogs()
        }

        // Start consuming and writing logs
        scope.launch {
            logChannel.consumeEach { logContent ->
                writeToFile(logContent)
            }
        }
    }

    override fun log(priority: Int, tag: String?, message: String, t: Throwable?) {
        // Always ignore verbose logs to save space
        if (priority < Log.DEBUG) return

        // --- Core Filtering Logic ---
        // If it's a Release build, strictly filter out any tag not defined in LogFormatter
        if (!BuildConfig.DEBUG && tag !in allowedReleaseTags) {
            return
        }

        val timestamp = dateFormat.format(Date())
        val priorityStr = priorityToString(priority)
        val finalTag = tag ?: "Unknown"

        val logLine = "$timestamp $priorityStr/$finalTag: $message\n"
        val stackTrace = t?.let { Log.getStackTraceString(it) + "\n" } ?: ""

        // Send to channel without blocking the main thread
        logChannel.trySend(logLine + stackTrace)
    }

    private fun writeToFile(content: String) {
        try {
            ensureLogDir()
            var file = getLogFile()

            // Check if the current file exceeds the 4MB limit
            if (file.exists() && file.length() > MAX_FILE_SIZE) {
                // Rotate the file by appending a timestamp suffix instead of deleting it
                val timestampSuffix = timeSuffixFormat.format(Date())
                val rotatedName = "${fileNameFormat.format(Date())}$timestampSuffix.log"
                val rotatedFile = File(logDir, rotatedName)

                // Rename the oversized file, allowing a fresh .log file to be created next
                file.renameTo(rotatedFile)
                file = getLogFile()
            }

            file.appendText(content)
        } catch (e: Exception) {
            Log.e("FileLoggingTree", "Error writing log", e)
        }
    }

    private fun cleanUpOldLogs() {
        try {
            if (!logDir.exists()) return
            // Find all files ending with .log
            val logFiles = logDir.listFiles { _, name -> name.endsWith(".log") } ?: return

            val cutoffTime = System.currentTimeMillis() - MAX_RETAIN_DAYS_MS

            for (file in logFiles) {
                // Delete files older than the 7-day retention threshold
                if (file.lastModified() < cutoffTime) {
                    file.delete()
                }
            }
        } catch (e: Exception) {
            Log.e("FileLoggingTree", "Error cleaning old logs", e)
        }
    }

    private fun getLogFile(): File {
        // Generate the base filename for the current day
        val fileName = "${fileNameFormat.format(Date())}.log"
        return File(logDir, fileName)
    }

    private fun ensureLogDir() {
        if (!logDir.exists()) logDir.mkdirs()
    }

    private fun priorityToString(priority: Int) = when (priority) {
        Log.VERBOSE -> "V"
        Log.DEBUG -> "D"
        Log.INFO -> "I"
        Log.WARN -> "W"
        Log.ERROR -> "E"
        Log.ASSERT -> "A"
        else -> "?"
    }

    fun release() {
        // Cancel the coroutine scope and close the channel to prevent memory leaks
        scope.cancel()
        logChannel.close()
    }
}