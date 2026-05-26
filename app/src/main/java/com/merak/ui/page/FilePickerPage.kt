package com.merak.ui.page

import android.os.Environment
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.InsertDriveFile
import androidx.compose.material.icons.filled.Folder
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.merak.ui.components.MtzInstallDialog
import com.merak.ui.icons.AppIcons
import com.merak.x.R
import top.yukonga.miuix.kmp.basic.Card
import top.yukonga.miuix.kmp.basic.Icon
import top.yukonga.miuix.kmp.basic.IconButton
import top.yukonga.miuix.kmp.basic.MiuixScrollBehavior
import top.yukonga.miuix.kmp.basic.Scaffold
import top.yukonga.miuix.kmp.basic.Text
import top.yukonga.miuix.kmp.basic.TopAppBar
import top.yukonga.miuix.kmp.basic.rememberTopAppBarState
import top.yukonga.miuix.kmp.theme.MiuixTheme
import top.yukonga.miuix.kmp.utils.overScrollVertical
import timber.log.Timber
import java.io.File

data class FileItem(
    val file: File,
    val isDirectory: Boolean,
    val name: String,
    val isMtzFile: Boolean = false
)

@Composable
fun FilePickerPage(
    onBack: () -> Unit,
    onFileSelected: (File, Long) -> Unit
) {
    var currentPath by remember {
        mutableStateOf(Environment.getExternalStorageDirectory())
    }
    var fileItems by remember { mutableStateOf<List<FileItem>>(emptyList()) }
    val scrollBehavior = MiuixScrollBehavior(rememberTopAppBarState())
    var showDialog = remember { mutableStateOf(false) }
    var selectedMtzFile by remember { mutableStateOf<File?>(null) }
    val checkedStates = remember { mutableStateMapOf<Long, Boolean>() }

    // 加载当前目录的文件列表
    LaunchedEffect(currentPath) {
        loadFiles(currentPath) { items ->
            fileItems = items
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = currentPath.name.ifEmpty { "存储" },
                scrollBehavior = scrollBehavior,
                navigationIcon = {
                    IconButton(
                        onClick = {
                            // 如果在根目录，返回上一页；否则返回上级目录
                            if (currentPath.parent != null &&
                                currentPath != Environment.getExternalStorageDirectory()
                            ) {
                                currentPath = currentPath.parentFile ?: currentPath
                            } else {
                                onBack()
                            }
                        },
                        modifier = Modifier.padding(start = 18.dp)
                    ) {
                        Icon(
                            imageVector = AppIcons.Back,
                            contentDescription = "返回"
                        )
                    }
                }
            )
        }
    ) { padding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .nestedScroll(scrollBehavior.nestedScrollConnection)
                .overScrollVertical()
                .padding(horizontal = 12.dp)
        ) {
            // 显示当前路径
            item {
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 8.dp)
                ) {
                    Text(
                        text = currentPath.absolutePath,
                        fontSize = 13.sp,
                        color = Color.Gray,
                        modifier = Modifier.padding(12.dp)
                    )
                }
            }

            // 显示文件和文件夹列表
            items(fileItems, key = { it.file.absolutePath }) { item ->
                FileItemView(
                    item = item,
                    onClick = {
                        if (item.isDirectory) {
                            // 进入文件夹
                            currentPath = item.file
                        } else if (item.isMtzFile) {
                            // 选择 .mtz 文件
                            selectedMtzFile = item.file
                        }
                    }
                )
            }

            // 空状态提示
            if (fileItems.isEmpty()) {
                item {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 32.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Text(
                            text = stringResource(R.string.empty_folder),
                            fontSize = 14.sp,
                            color = Color.Gray
                        )
                    }
                }
            }
        }
    }

    selectedMtzFile?.let { file ->
        MtzInstallDialog(
            fileName = file.name,
            onDismissRequest = {
                // Clear the state to close the dialog
                selectedMtzFile = null
            },
            onInstallConfirm = { flags ->
                // Pass the result to the ViewModel and close the dialog
                onFileSelected(file, flags)
                selectedMtzFile = null
            }
        )
    }
}

@Composable
fun FileItemView(
    item: FileItem,
    onClick: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp)
            .clickable(onClick = onClick)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // 图标
            Icon(
                imageVector = if (item.isDirectory) {
                    Icons.Default.Folder
                } else {
                    Icons.AutoMirrored.Filled.InsertDriveFile
                },
                contentDescription = null,
                tint = when {
                    item.isMtzFile -> Color(0xFF3482FF)
                    item.isDirectory -> Color(0xFFFFA726)
                    else -> Color.Gray
                },
                modifier = Modifier.size(28.dp)
            )

            Spacer(modifier = Modifier.width(12.dp))

            // 文件名
            Column(
                modifier = Modifier.weight(1f)
            ) {
                Text(
                    text = item.name,
                    fontSize = 15.sp,
                    fontWeight = if (item.isMtzFile) FontWeight.Bold else FontWeight.Normal,
                    color = if (item.isMtzFile) {
                        Color(0xFF3482FF)
                    } else {
                        MiuixTheme.colorScheme.onSurface
                    }
                )

                // 显示文件大小（仅文件）
                if (!item.isDirectory && item.file.exists()) {
                    Text(
                        text = formatFileSize(item.file.length()),
                        fontSize = 12.sp,
                        color = Color.Gray
                    )
                }
            }

            // MTZ 文件标识
            if (item.isMtzFile) {
                Text(
                    text = "MTZ",
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color.White,
                    modifier = Modifier
                        .background(
                            Color(0xFF3482FF),
                            shape = androidx.compose.foundation.shape.RoundedCornerShape(4.dp)
                        )
                        .padding(horizontal = 6.dp, vertical = 2.dp)
                )
            }
        }
    }
}

private fun loadFiles(directory: File, onLoaded: (List<FileItem>) -> Unit) {
    try {
        val files = directory.listFiles()?.filter { file ->
            // 过滤隐藏文件和特殊目录
            !file.name.startsWith(".")
        }?.sortedWith(
            compareBy(
                // 文件夹排在前面
                { !it.isDirectory },
                // 按名称排序
                { it.name.lowercase() }
            ))?.map { file ->
            FileItem(
                file = file,
                isDirectory = file.isDirectory,
                name = file.name,
                isMtzFile = !file.isDirectory && file.extension.equals("mtz", ignoreCase = true)
            )
        } ?: emptyList()

        onLoaded(files)
    } catch (e: Exception) {
        Timber.tag("FilePickerPage").e(e, "Failed to load files from %s", directory.absolutePath)
        onLoaded(emptyList())
    }
}

private fun formatFileSize(size: Long): String {
    return when {
        size < 1024 -> "$size B"
        size < 1024 * 1024 -> "${size / 1024} KB"
        size < 1024 * 1024 * 1024 -> "${size / (1024 * 1024)} MB"
        else -> "${size / (1024 * 1024 * 1024)} GB"
    }
}

