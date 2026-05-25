package com.merak.ui.page.home.log

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.selection.SelectionContainer
import androidx.compose.foundation.verticalScroll
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.merak.ui.components.MiuixBackButton
import com.merak.ui.icons.AppIcons
import com.merak.ui.theme.LocalSemanticColors
import com.merak.x.R
import org.koin.androidx.compose.koinViewModel
import top.yukonga.miuix.kmp.basic.ButtonDefaults
import top.yukonga.miuix.kmp.basic.Card
import top.yukonga.miuix.kmp.basic.CardDefaults
import top.yukonga.miuix.kmp.basic.Icon
import top.yukonga.miuix.kmp.basic.IconButton
import top.yukonga.miuix.kmp.basic.MiuixScrollBehavior
import top.yukonga.miuix.kmp.basic.Scaffold
import top.yukonga.miuix.kmp.basic.SmallTitle
import top.yukonga.miuix.kmp.basic.Text
import top.yukonga.miuix.kmp.basic.TextButton
import top.yukonga.miuix.kmp.basic.TopAppBar
import top.yukonga.miuix.kmp.theme.MiuixTheme
import top.yukonga.miuix.kmp.utils.PressFeedbackType
import top.yukonga.miuix.kmp.utils.overScrollVertical
import top.yukonga.miuix.kmp.utils.scrollEndHaptic
import top.yukonga.miuix.kmp.window.WindowDialog

@Composable
fun LogPage(
    onBack: () -> Unit,
    viewModel: LogViewModel = koinViewModel()
) {
    val uiState by viewModel.uiState.collectAsState()
    val scrollBehavior = MiuixScrollBehavior()
    val showClearDialog = remember { mutableStateOf(false) }
    var selectedLog by remember { mutableStateOf<LogEntry?>(null) }
    val colors = LocalSemanticColors.current

    // Clear confirmation dialog
    WindowDialog(
        title = stringResource(R.string.log_clear_confirm_title),
        summary = stringResource(R.string.log_clear_confirm_message),
        show = showClearDialog.value,
        onDismissRequest = { showClearDialog.value = false }
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            TextButton(
                text = stringResource(R.string.cancel),
                onClick = { showClearDialog.value = false },
                modifier = Modifier.weight(1f)
            )
            Spacer(Modifier.width(20.dp))
            TextButton(
                text = stringResource(R.string.confirm),
                onClick = {
                    viewModel.clearLogs()
                    showClearDialog.value = false
                },
                modifier = Modifier.weight(1f),
                colors = ButtonDefaults.textButtonColorsPrimary()
            )
        }
    }

    if (selectedLog != null) {
        LogDetailDialog(
            log = selectedLog!!,
            onDismiss = { selectedLog = null }
        )
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = stringResource(R.string.log_title),
                scrollBehavior = scrollBehavior,
                navigationIcon = {
                    MiuixBackButton(
                        modifier = Modifier.padding(start = 16.dp),
                        onClick = onBack
                    )
                },
                actions = {
                    IconButton(
                        modifier = Modifier.padding(end = 16.dp),
                        onClick = { showClearDialog.value = true }) {
                        Icon(imageVector = AppIcons.Delete, contentDescription = "Clear Logs")
                    }
                }
            )
        }
    ) { innerPadding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .overScrollVertical()
                .scrollEndHaptic()
                .nestedScroll(scrollBehavior.nestedScrollConnection),
            contentPadding = PaddingValues(
                top = innerPadding.calculateTopPadding(),
                bottom = innerPadding.calculateBottomPadding()
            )
        ) {
            // 1. Statistics header
            item { SmallTitle(stringResource(R.string.log_stats_title)) }

            // 2. Running statistics card area
            item { StatisticsSection(uiState.statistics) }

            // 3. Log list header
            item { SmallTitle(stringResource(R.string.log_list_title)) }

            if (uiState.logs.isEmpty()) {
                item {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(200.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = if (uiState.isLoading) stringResource(R.string.loading) else stringResource(R.string.log_empty),
                            color = colors.infoText
                        )
                    }
                }
            } else {
                items(uiState.logs) { log ->
                    LogItemView(log, onClick = { selectedLog = log })
                }
            }

            item { Spacer(modifier = Modifier.navigationBarsPadding()) }
        }
    }
}

/**
 * Statistics area (White large card wrapping overview + two colored cards)
 */
@Composable
fun StatisticsSection(stats: LogStatistics) {
    val colors = LocalSemanticColors.current

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 12.dp)
            .padding(bottom = 6.dp)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            // Top: Running statistics & size
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text(
                        text = stringResource(R.string.log_statistics_card_title),
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Bold,
                        color = MiuixTheme.colorScheme.onSurface
                    )
                    Spacer(modifier = Modifier.height(2.dp))
                    Text(
                        text = stringResource(R.string.log_total_count, stats.totalCount),
                        fontSize = 12.sp,
                        color = colors.infoText
                    )
                }

                Column(horizontalAlignment = Alignment.End) {
                    Text(
                        text = formatFileSize(stats.totalSize),
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Bold,
                        color = colors.linkText
                    )
                    Spacer(modifier = Modifier.height(2.dp))
                    Text(
                        text = stringResource(R.string.log_file_size_label),
                        fontSize = 12.sp,
                        color = colors.infoText
                    )
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Bottom: Two colored statistics blocks
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                // Theme installation statistics
                StatItemCard(
                    modifier = Modifier.weight(1f),
                    title = stringResource(R.string.log_label_theme_install),
                    count = stats.installCount,
                    time = stats.lastInstallTime,
                    bgColor = colors.successBg,
                    textColor = colors.successText
                )

                // Broadcast intercept statistics
                StatItemCard(
                    modifier = Modifier.weight(1f),
                    title = stringResource(R.string.log_label_alarm_intercept),
                    count = stats.interceptCount,
                    time = stats.lastInterceptTime,
                    bgColor = colors.warningBg,
                    textColor = colors.warningText
                )
            }
        }
    }
}

/**
 * Single colored statistics card
 */
@Composable
fun StatItemCard(
    modifier: Modifier = Modifier,
    title: String,
    count: Int,
    time: String,
    bgColor: Color,
    textColor: Color
) {
    val colors = LocalSemanticColors.current

    Box(
        modifier = modifier
            .height(100.dp)
            .clip(RoundedCornerShape(12.dp))
            .background(bgColor),
        contentAlignment = Alignment.Center
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text(
                text = title,
                fontSize = 13.sp,
                color = colors.infoText,
                fontWeight = FontWeight.Medium
            )
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = count.toString(),
                fontSize = 28.sp,
                color = textColor,
                fontWeight = FontWeight.Bold
            )
            Text(
                text = stringResource(R.string.log_count_unit),
                fontSize = 10.sp,
                color = colors.infoText
            )
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = time,
                fontSize = 10.sp,
                color = colors.infoText.copy(alpha = 0.8f)
            )
        }
    }
}

/**
 * Log list item
 */
@Composable
fun LogItemView(
    log: LogEntry,
    onClick: () -> Unit // Add click callback
) {
    val colors = LocalSemanticColors.current

    // Color scheme logic: Using Bg and Text pairs to maintain contrast in both modes
    val (tagBgColor, tagTextColor) = when (log.tagType) {
        LogTagType.THEME_INSTALL -> colors.successBg to colors.successText
        LogTagType.ALARM_INTERCEPT -> colors.warningBg to colors.warningText
        LogTagType.ERROR -> colors.errorBg to colors.errorText
        LogTagType.WARNING -> colors.warningBg to colors.warningText
        else -> colors.infoBg to colors.infoTextDark
    }

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 12.dp)
            .padding(bottom = 6.dp),
        onClick = onClick,
        pressFeedbackType = PressFeedbackType.None,
        showIndication = true,
    ) {
        Column(
            modifier = Modifier
                .background(if (log.tagType == LogTagType.ERROR) colors.errorBg.copy(alpha = 0.3f) else Color.Transparent)
                .padding(16.dp)
        ) {
            // Header
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = log.displayTime,
                    fontSize = 13.sp,
                    color = colors.infoText,
                    fontFamily = FontFamily.Monospace,
                    fontWeight = FontWeight.Medium
                )

                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(4.dp))
                        .background(tagBgColor)
                        .padding(horizontal = 8.dp, vertical = 2.dp)
                ) {
                    Text(
                        text = log.tag,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        color = tagTextColor
                    )
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            // Title
            Text(
                text = log.title,
                fontSize = 15.sp,
                fontWeight = FontWeight.Bold,
                color = if (log.tagType == LogTagType.ERROR) colors.errorText else MiuixTheme.colorScheme.onSurface,
                maxLines = 1, // Limit title to 1 line
                overflow = TextOverflow.Ellipsis
            )

            // Content (Collapsed display)
            if (log.content.isNotEmpty()) {
                Spacer(modifier = Modifier.height(6.dp))
                Text(
                    text = log.content,
                    fontSize = 12.sp,
                    color = if (log.tagType == LogTagType.ERROR) colors.errorText.copy(alpha = 0.8f) else colors.infoText,
                    fontFamily = FontFamily.Monospace,
                    lineHeight = 16.sp,
                    maxLines = 4, // Key: Limit lines, collapse by default
                    overflow = TextOverflow.Ellipsis
                )
            }
        }
    }
}

/**
 * Detail dialog component
 */
@Composable
fun LogDetailDialog(
    log: LogEntry,
    onDismiss: () -> Unit
) {
    val colors = LocalSemanticColors.current

    // Use a simple Boolean State to control display, coordinating with SuperDialog logic
    val showState = remember { mutableStateOf(true) }
    val context = LocalContext.current

    // Synchronize external state when SuperDialog requests close internally
    if (!showState.value) {
        onDismiss()
    }

    WindowDialog(
        title = log.title.ifEmpty { "日志详情" },
        show = showState.value,
        onDismissRequest = { onDismiss() }
    ) {
        Column {
            // Time and tag information
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.padding(bottom = 8.dp)
            ) {
                Text(
                    text = log.displayTime,
                    fontSize = 12.sp,
                    color = colors.infoText,
                    fontFamily = FontFamily.Monospace
                )
                Spacer(modifier = Modifier.width(8.dp))

                // Reuse Tag style logic (Simplified version)
                val tagColor = when (log.tagType) {
                    LogTagType.ERROR -> colors.errorText
                    LogTagType.WARNING -> colors.warningText
                    LogTagType.THEME_INSTALL -> colors.successText
                    else -> colors.infoTextDark
                }
                Text(
                    text = "[${log.tag}]",
                    fontSize = 12.sp,
                    color = tagColor,
                    fontWeight = FontWeight.Bold
                )
            }

            // Log content (Scrollable, selectable for copy)
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(max = 350.dp), // Limit maximum height to prevent dialog from being too long
                cornerRadius = 0.dp,
                colors = CardDefaults.defaultColors(
                    color = Color.Transparent,
                    contentColor = MiuixTheme.colorScheme.onSurface
                )
            ) {
                SelectionContainer { // Allow long press to select text
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .verticalScroll(rememberScrollState())
                    ) {
                        Text(
                            text = log.content.ifEmpty { "无详细内容" },
                            fontSize = 12.sp,
                            fontFamily = FontFamily.Monospace, // Monospaced font for stack trace display
                            color = if (log.tagType == LogTagType.ERROR) colors.errorText else MiuixTheme.colorScheme.onSurface,
                            lineHeight = 16.sp
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Bottom button group
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                // Close button
                TextButton(
                    text = stringResource(R.string.close),
                    onClick = { showState.value = false },
                    modifier = Modifier.weight(1f)
                )

                Spacer(modifier = Modifier.width(12.dp))

                // Copy button
                TextButton(
                    text = stringResource(R.string.copy),
                    onClick = {
                        copyToClipboard(context, "${log.title}\n${log.content}")
                        showState.value = false
                    },
                    modifier = Modifier.weight(1f),
                    colors = ButtonDefaults.textButtonColorsPrimary()
                )
            }
        }
    }
}

private fun copyToClipboard(context: Context, text: String) {
    val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
    val clip = ClipData.newPlainText("Log", text)
    clipboard.setPrimaryClip(clip)
    Toast.makeText(context, "日志已复制", Toast.LENGTH_SHORT).show()
}

private fun formatFileSize(size: Long): String {
    return when {
        size < 1024 -> "$size B"
        size < 1024 * 1024 -> "${size / 1024} KB"
        else -> String.format("%.2f MB", size / (1024.0 * 1024.0))
    }
}
