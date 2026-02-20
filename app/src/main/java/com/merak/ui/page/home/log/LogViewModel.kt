package com.merak.ui.page.home.log

import android.app.Application
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.merak.service.KeepAliveService
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

// 统计数据模型
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
    NORMAL,         // 普通 Debug/Info
    THEME_INSTALL,  // 绿色：主题安装
    ALARM_INTERCEPT,// 橙色：广播拦截
    WARNING,        // 黄色：警告
    ERROR           // 红色：错误/异常
}

data class LogEntry(
    val rawTimestamp: Long,
    val displayTime: String,
    val tag: String,
    val tagType: LogTagType,
    val title: String,
    val content: String,
    val isExpanded: Boolean = false // 预留：点击展开堆栈
)

class LogViewModel(
    private val context: Application
) : ViewModel() {

    private val _uiState = MutableStateFlow(LogUiState())
    val uiState = _uiState.asStateFlow()

    private val logDir by lazy { File(context.filesDir, "logs") }

    // [列表用] 短时间格式: 02-09 12:22:08
    private val displayDateFormat = SimpleDateFormat("MM-dd HH:mm:ss", Locale.getDefault())

    // [统计卡片用] 长时间格式: 2026-02-09 12:22
    private val statsDateFormat = SimpleDateFormat("yyyy-MM-dd HH:mm", Locale.getDefault())

    // 匹配日志文件行首的时间戳: 2024-02-09 12:22:08.123
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

        // 获取所有的日志文件，而不是只取 latestFile
        val files = logDir.listFiles { _, name -> name.endsWith(".log") } ?: emptyArray()

        if (files.isEmpty()) {
            _uiState.value = LogUiState(isLoading = false)
            return@withContext
        }

        val allEntries = mutableListOf<LogEntry>()
        var totalSize = 0L

        // 1. 遍历并解析所有日志文件
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
                e.printStackTrace()
            }
        }

        // 2. 将所有日志按时间戳进行升序排序 (旧的在前，新的在后)
        allEntries.sortBy { it.rawTimestamp }

        var installCount = 0
        var interceptCount = 0
        var lastInstallTime = "-"
        var lastInterceptTime = "-"

        // 3. 统计过程
        allEntries.forEach { entry ->
            if (entry.tagType == LogTagType.THEME_INSTALL) {
                installCount++
                lastInstallTime = statsDateFormat.format(Date(entry.rawTimestamp))
            } else if (entry.tagType == LogTagType.ALARM_INTERCEPT) {
                interceptCount++
                lastInterceptTime = statsDateFormat.format(Date(entry.rawTimestamp))
            }
        }

        // 4. 列表倒序显示（最新的在最上面）
        val reversedEntries = allEntries.reversed()

        _uiState.value = LogUiState(
            logs = reversedEntries,
            isLoading = false,
            statistics = LogStatistics(
                totalCount = allEntries.size,
                totalSize = totalSize, // 所有日志文件的总大小
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
            KeepAliveService.requestRefresh(context)
        }
    }

    // 辅助类，用于构建 LogEntry
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
            // 格式: "2024-02-09 12:00:00.000 D/Tag: Title | Content"
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

            // 1. 识别类型 (使用 LogFormatter 常量)
            val type = when (tag) {
                LogFormatter.TAG_THEME_INSTALL -> LogTagType.THEME_INSTALL
                LogFormatter.TAG_ALARM_INTERCEPT -> LogTagType.ALARM_INTERCEPT
                LogFormatter.TAG_CRASH -> LogTagType.ERROR
                else -> {
                    if (level == "E") LogTagType.ERROR
                    else if (level == "W") LogTagType.WARNING
                    else LogTagType.NORMAL
                }
            }

            // 2. 分离标题和内容 (使用 | 分隔符)
            val (title, content) = if (messageBody.contains(" | ")) {
                val splitMsg = messageBody.split(" | ", limit = 2)
                splitMsg[0] to splitMsg[1]
            } else {
                // 回退策略：如果没有分隔符，尝试换行符，或者全部作为标题/内容
                if (messageBody.contains("\n")) {
                    val splitMsg = messageBody.split("\n", limit = 2)
                    splitMsg[0] to splitMsg[1]
                } else {
                    // 对于普通日志，全部作为标题显示
                    messageBody to ""
                }
            }

            return LogEntryBuilder(rawTime, displayTime, tag, type, title, content)
        } catch (e: Exception) {
            return null
        }
    }
}