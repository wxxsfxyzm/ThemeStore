package com.merak.ui.components

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.merak.core.installer.ThemeFlags
import com.merak.core.installer.ThemeModuleProvider.installModules
import com.merak.ui.util.WindowBlurEffect
import top.yukonga.miuix.kmp.basic.ButtonDefaults
import top.yukonga.miuix.kmp.basic.Card
import top.yukonga.miuix.kmp.basic.Text
import top.yukonga.miuix.kmp.basic.TextButton
import top.yukonga.miuix.kmp.extra.WindowDialog
import top.yukonga.miuix.kmp.theme.MiuixTheme

@Composable
fun MtzInstallDialog(
    useWindowBlur: Boolean = false,
    fileName: String,
    onDismissRequest: () -> Unit,
    onInstallConfirm: (Long) -> Unit
) {
    val showDialog = remember { mutableStateOf(true) }
    val checkedStates = remember { mutableStateMapOf<Long, Boolean>() }

    WindowBlurEffect(useWindowBlur)

    // Initialize default checked states
    LaunchedEffect(Unit) {
        checkedStates.clear()
        installModules.forEach { checkedStates[it.flag] = it.isDefaultChecked }
    }

    WindowDialog(
        title = "安装选项",
        show = showDialog,
        onDismissRequest = {
            showDialog.value = false
            onDismissRequest()
        }
    ) {
        Column {
            Text(
                text = "文件: $fileName",
                fontSize = 13.sp,
                color = Color.Gray,
                modifier = Modifier.padding(bottom = 8.dp)
            )

            val isAllChecked = installModules.all { checkedStates[it.flag] == true }

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 8.dp)
                    .clickable {
                        val newState = !isAllChecked
                        installModules.forEach { checkedStates[it.flag] = newState }
                    },
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = if (isAllChecked) "取消全选" else "全选所有",
                    color = MiuixTheme.colorScheme.primary,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.weight(1f)
                )
            }

            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(max = 300.dp),
            ) {
                Column(
                    modifier = Modifier.verticalScroll(rememberScrollState())
                ) {
                    installModules.forEach { module ->
                        MiuixCheckboxWidget(
                            title = module.name,
                            checked = checkedStates[module.flag] == true,
                            onCheckedChange = { isChecked ->
                                checkedStates[module.flag] = isChecked
                            }
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                TextButton(
                    text = "取消",
                    onClick = {
                        showDialog.value = false
                        onDismissRequest()
                    },
                    modifier = Modifier.weight(1f)
                )
                Spacer(modifier = Modifier.width(16.dp))
                TextButton(
                    text = "安装",
                    onClick = {
                        showDialog.value = false
                        var finalFlag = 0L
                        var checkCount = 0

                        installModules.forEach { module ->
                            if (checkedStates[module.flag] == true) {
                                finalFlag = finalFlag or module.flag
                                checkCount++
                            }
                        }

                        if (finalFlag != 0L) {
                            if (checkCount == installModules.size) {
                                finalFlag = ThemeFlags.ALL
                            }
                            onInstallConfirm(finalFlag)
                        } else {
                            // If nothing is selected, treat it as canceled
                            onDismissRequest()
                        }
                    },
                    modifier = Modifier.weight(1f),
                    colors = ButtonDefaults.textButtonColorsPrimary()
                )
            }
        }
    }
}