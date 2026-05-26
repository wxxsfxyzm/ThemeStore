package com.merak.ui.page.home.log

import android.app.Application
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.merak.service.ThemeInstallAccessibilityService
import com.merak.util.timber.LogFormatter
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import timber.log.Timber

// Statistics data model
data class LogStatistics(
    val totalCount: Int = 0,
    val totalSize: Long = 0,
    val installCount: Int = 0,
    val lastInstallTime: String = "-",
    val interceptCount: Int = 0,
    val lastInterceptTime: String = "-"
)

data class LogUiState(
    val logs: List<LogEntry> = emptyList(),
    val statistics: LogStatistics = LogStatistics(),
    val isLoading: Boolean = true
)

enum class LogTagType {
    NORMAL,         // Normal Debug/Info
    THEME_INSTALL,  // Green: Theme installation
    ALARM_INTERCEPT,// Orange: Broadcast interception
    WARNING,        // Yellow: Warning
    ERROR           // Red: Error/Exception
}

data class LogEntry(
    val rawTimestamp: Long,
    val displayTime: String,
    val tag: String,
    val tagType: LogTagType,
    val title: String,
    val content: String,
    val isExpanded: Boolean = false // Reserved: Click to expand stack trace
)

class LogViewModel(
    private val context: Application
) : ViewModel() {

    private val _uiState = MutableStateFlow(LogUiState())
    val uiState = _uiState.asStateFlow()

    private val logDir by lazy { File(context.filesDir, "logs") }

    // [For list] Short time format: 02-09 12:22:08
    private val displayDateFormat = SimpleDateFormat("MM-dd HH:mm:ss", Locale.getDefault())

    // [For stats card] Long time format: 2026-02-09 12:22
    private val statsDateFormat = SimpleDateFormat("yyyy-MM-dd HH:mm", Locale.getDefault())

    // Regex to match timestamp at the beginning of log file line: 2024-02-09 12:22:08.123
    private val timeRegex = Regex("^\\d{4}-\\d{2}-\\d{2}\\s\\d{2}:\\d{2}:\\d{2}\\.\\d{3}")
    private val logFileDateFormat = SimpleDateFormat("yyyy-MM-dd HH:mm:ss.SSS", Locale.US)

    init {
        startAutoRefresh()
    }

    private fun startAutoRefresh() {
        viewModelScope.launch {
            while (true) {
                refreshLogs()
                delay(3000)
            }
        }
    }

    suspend fun refreshLogs() = withContext(Dispatchers.IO) {
        if (!logDir.exists()) {
            _uiState.value = LogUiState(isLoading = false)
            return@withContext
        }

        // Get all log files instead of just the latestFile
        val files = logDir.listFiles { _, name -> name.endsWith(".log") } ?: emptyArray()

        if (files.isEmpty()) {
            _uiState.value = LogUiState(isLoading = false)
            return@withContext
        }

        val allEntries = mutableListOf<LogEntry>()
        var totalSize = 0L

        // 1. Traverse and parse all log files
        files.forEach { file ->
            totalSize += file.length()
            try {
                val lines = file.readLines()
                var currentEntryBuilder: LogEntryBuilder? = null

                for (line in lines) {
                    if (timeRegex.containsMatchIn(line)) {
                        currentEntryBuilder?.let { allEntries.add(it.build()) }
                        currentEntryBuilder = parseLogHeader(line)
                    } else {
                        currentEntryBuilder?.appendContent("\n$line")
                    }
                }
                currentEntryBuilder?.let { allEntries.add(it.build()) }

            } catch (e: Exception) {
                Timber.tag(TAG).e(e, "Failed to read log file: %s", file.absolutePath)
            }
        }

        // 2. Sort all logs by timestamp in ascending order (oldest first, newest last)
        allEntries.sortBy { it.rawTimestamp }

        var installCount = 0
        var interceptCount = 0
        var lastInstallTime = "-"
        var lastInterceptTime = "-"

        // 3. Statistics process
        allEntries.forEach { entry ->
            if (entry.tagType == LogTagType.THEME_INSTALL) {
                installCount++
                lastInstallTime = statsDateFormat.format(Date(entry.rawTimestamp))
            } else if (entry.tagType == LogTagType.ALARM_INTERCEPT) {
                interceptCount++
                lastInterceptTime = statsDateFormat.format(Date(entry.rawTimestamp))
            }
        }

        // 4. Display list in reverse order (newest on top)
        val reversedEntries = allEntries.reversed()

        _uiState.value = LogUiState(
            logs = reversedEntries,
            isLoading = false,
            statistics = LogStatistics(
                totalCount = allEntries.size,
                totalSize = totalSize, // Total size of all log files
                installCount = installCount,
                lastInstallTime = lastInstallTime,
                interceptCount = interceptCount,
                lastInterceptTime = lastInterceptTime
            )
        )
    }

    fun clearLogs() {
        viewModelScope.launch(Dispatchers.IO) {
            logDir.listFiles()?.forEach { it.delete() }
            refreshLogs()
            // Dispatch a refresh signal to update notification stats
            ThemeInstallAccessibilityService.requestRefresh()
        }
    }

    // Helper class for building LogEntry
    private class LogEntryBuilder(
        val rawTimestamp: Long,
        val displayTime: String,
        val tag: String,
        val tagType: LogTagType,
        val title: String,
        var content: String
    ) {
        fun appendContent(more: String) {
            content += more
        }

        fun build() = LogEntry(rawTimestamp, displayTime, tag, tagType, title, content.trim())
    }

    private fun parseLogHeader(line: String): LogEntryBuilder? {
        try {
            // Format: "2024-02-09 12:00:00.000 D/Tag: Title | Content"
            val parts = line.split(" ", limit = 3)
            if (parts.size < 3) return null

            val rawTimeStr = "${parts[0]} ${parts[1]}"
            val rawTime = logFileDateFormat.parse(rawTimeStr)?.time ?: 0L
            val displayTime = displayDateFormat.format(rawTime)

            val rest = parts[2] // "D/Tag: Title | Content"
            val levelTagSplit = rest.split(":", limit = 2)
            if (levelTagSplit.size < 2) return null

            val levelTag = levelTagSplit[0] // "D/TAG"
            val messageBody = levelTagSplit[1].trim() // "Title | Content"

            val level = levelTag.substringBefore("/")
            val tag = levelTag.substringAfter("/")

            // 1. Identify type (using LogFormatter constants)
            val type = when (tag) {
                LogFormatter.TAG_THEME_INSTALL -> LogTagType.THEME_INSTALL
                LogFormatter.TAG_ALARM_INTERCEPT -> LogTagType.ALARM_INTERCEPT
                LogFormatter.TAG_CRASH,
                LogFormatter.TAG_ERROR -> LogTagType.ERROR

                else -> {
                    when (level) {
                        "E" -> LogTagType.ERROR
                        "W" -> LogTagType.WARNING
                        else -> LogTagType.NORMAL
                    }
                }
            }

            // 2. Separate title and content (using | separator)
            val (title, content) = if (messageBody.contains(" | ")) {
                val splitMsg = messageBody.split(" | ", limit = 2)
                splitMsg[0] to splitMsg[1]
            } else {
                // Fallback strategy: if there is no separator, try newline, or use all as title/content
                if (messageBody.contains("\n")) {
                    val splitMsg = messageBody.split("\n", limit = 2)
                    splitMsg[0] to splitMsg[1]
                } else {
                    // For normal logs, display everything as title
                    messageBody to ""
                }
            }

            return LogEntryBuilder(rawTime, displayTime, tag, type, title, content)
        } catch (e: Exception) {
            return null
        }
    }

    private companion object {
        const val TAG = "LogViewModel"
    }
}
