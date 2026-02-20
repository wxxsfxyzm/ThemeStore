package com.merak.ui.page.home

import android.content.Intent
import android.provider.Settings
import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.painter.Painter
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.Lifecycle
import androidx.navigation.NavController
import com.merak.ui.Route
import com.merak.ui.components.OnLifecycleEvent
import com.merak.ui.theme.getMiuixAppBarColor
import com.merak.ui.theme.rememberMiuixHazeStyle
import com.merak.ui.theme.tsHazeEffect
import com.merak.x.R
import dev.chrisbanes.haze.HazeState
import dev.chrisbanes.haze.hazeSource
import org.koin.androidx.compose.koinViewModel
import rikka.shizuku.Shizuku
import top.yukonga.miuix.kmp.basic.Card
import top.yukonga.miuix.kmp.basic.Icon
import top.yukonga.miuix.kmp.basic.MiuixScrollBehavior
import top.yukonga.miuix.kmp.basic.Scaffold
import top.yukonga.miuix.kmp.basic.SmallTitle
import top.yukonga.miuix.kmp.basic.TopAppBar
import top.yukonga.miuix.kmp.extra.SuperArrow
import top.yukonga.miuix.kmp.theme.MiuixTheme
import top.yukonga.miuix.kmp.utils.overScrollVertical
import top.yukonga.miuix.kmp.utils.scrollEndHaptic

@Composable
fun HomePage(
    navController: NavController,
    viewModel: HomeViewModel = koinViewModel(),
    hazeState: HazeState?,
    outerPadding: PaddingValues = PaddingValues(0.dp)
) {
    val context = LocalContext.current
    val uiState by viewModel.uiState.collectAsState()
    val scrollBehavior = MiuixScrollBehavior()
    val hazeStyle = rememberMiuixHazeStyle()

    OnLifecycleEvent(Lifecycle.Event.ON_RESUME) {
        viewModel.refreshState()
    }

    LaunchedEffect(Unit) {
        viewModel.effect.collect { effect ->
            // ... (保持原有的 effect 收集逻辑完全不变)
            when (effect) {
                is HomeSideEffect.OpenAccessibilitySettings -> {
                    val intent = Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS)
                    try {
                        context.startActivity(intent)
                    } catch (e: Exception) {
                        Toast.makeText(context, context.getString(R.string.about_open_failed), Toast.LENGTH_SHORT).show()
                    }
                }

                is HomeSideEffect.ShowToast -> Toast.makeText(context, effect.message, Toast.LENGTH_SHORT).show()
                is HomeSideEffect.ShowToastRes -> Toast.makeText(context, context.getString(effect.resId), Toast.LENGTH_SHORT).show()
                is HomeSideEffect.OpenShizukuManager -> {
                    val intent = context.packageManager.getLaunchIntentForPackage("moe.shizuku.privileged.api")
                    if (intent != null) context.startActivity(intent)
                    else Toast.makeText(context, context.getString(R.string.shizuku_not_installed), Toast.LENGTH_SHORT).show()
                }

                is HomeSideEffect.RequestShizukuPermission -> {
                    try {
                        Shizuku.requestPermission(effect.requestCode)
                    } catch (e: Exception) {
                        Toast.makeText(context, "Error: ${e.message}", Toast.LENGTH_SHORT).show()
                    }
                }
            }
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                modifier = Modifier.tsHazeEffect(hazeState, hazeStyle),
                color = hazeState.getMiuixAppBarColor(),
                title = stringResource(R.string.title_home),
                scrollBehavior = scrollBehavior
            )
        }
    ) { innerPadding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .then(hazeState?.let { Modifier.hazeSource(it) } ?: Modifier)
                .overScrollVertical()
                .scrollEndHaptic()
                .nestedScroll(scrollBehavior.nestedScrollConnection),
            contentPadding = PaddingValues(
                top = innerPadding.calculateTopPadding(),
                bottom = outerPadding.calculateBottomPadding()
            ),
            overscrollEffect = null
        ) {
            item { Spacer(modifier = Modifier.size(12.dp)) }

            // --- Accessibility Status ---
            item {
                val isAccEnabled = uiState.isAccessibilityEnabled
                val isKeepAlive = uiState.isKeepAliveRunning

                // 核心改动：细分描述文本的逻辑
                val accDescription = when {
                    !isAccEnabled -> stringResource(R.string.home_accessibility_status_off)
                    isKeepAlive -> "${stringResource(R.string.home_accessibility_status_on)} - 保活服务正在运行"
                    else -> "${stringResource(R.string.home_accessibility_status_on)} - 保活服务未运行"
                }

                // 核心改动：细分描述文本的颜色
                val accDescColor = when {
                    !isAccEnabled -> Color(0xFFD32F2F)
                    isKeepAlive -> Color(0xFF2E7D32)
                    else -> MiuixTheme.colorScheme.onSurfaceVariantSummary
                }

                StatusCard(
                    icon = painterResource(R.drawable.ic_accessibility),
                    title = stringResource(R.string.permission_accessibility_title),
                    isEnabled = isAccEnabled,
                    description = accDescription,
                    descriptionColor = accDescColor,
                    onClick = { viewModel.toggleAccessibilityService() }
                )
            }

            // --- Shizuku Status ---
            item {
                val isShizukuEnabled = uiState.isShizukuAuthorized
                val shizukuStatusText = when {
                    !uiState.isShizukuAvailable -> stringResource(R.string.shizuku_not_running)
                    !isShizukuEnabled -> stringResource(R.string.shizuku_not_authorized)
                    else -> stringResource(R.string.shizuku_running)
                }

                StatusCard(
                    icon = painterResource(R.drawable.ic_shizuku),
                    title = "Shizuku",
                    isEnabled = isShizukuEnabled,
                    description = shizukuStatusText,
                    descriptionColor = if (isShizukuEnabled) Color(0xFF2E7D32) else Color(0xFFD32F2F),
                    onClick = { viewModel.handleShizukuCardClick() }
                )
            }

            // ... (保持后面的 Theme Install 和 Tools Section 代码不变)
            item { SmallTitle(stringResource(R.string.home_install_title)) }
            item {
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 12.dp)
                        .padding(bottom = 6.dp)
                ) {
                    SuperArrow(
                        title = stringResource(R.string.home_install_title),
                        summary = stringResource(R.string.home_install_summary),
                        onClick = { navController.navigate(Route.THEME_INSTALL) }
                    )
                }
            }

            item { SmallTitle(stringResource(R.string.home_tools_title)) }
            item {
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 12.dp)
                        .padding(bottom = 6.dp)
                ) {
                    SuperArrow(
                        title = stringResource(R.string.log_title),
                        summary = stringResource(R.string.log_summary),
                        onClick = { navController.navigate(Route.LOG) }
                    )
                }
            }
        }
    }
}

/**
 * 重构后的 StatusCard 组件
 * 将 isEnabled 仅用于控制卡片主色调（背景、图标、状态圆角标）
 * 将 description 和 descriptionColor 完全开放以实现自定义文本层
 */
@Composable
private fun StatusCard(
    icon: Painter,
    title: String,
    isEnabled: Boolean,
    description: String,
    descriptionColor: Color,
    onClick: () -> Unit
) {
    // 决定卡片的“底层情绪颜色”：运行就是绿，没运行就是红
    val statusColor = if (isEnabled) Color(0xFF2E7D32) else Color(0xFFD32F2F)
    val backgroundColor = statusColor.copy(alpha = 0.1f)

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 12.dp, vertical = 4.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .background(backgroundColor) // 背景颜色
                .clickable(onClick = onClick)
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Icon section
            Icon(
                painter = icon,
                contentDescription = null,
                tint = statusColor, // 图标颜色跟随状态
                modifier = Modifier.size(24.dp)
            )

            Spacer(modifier = Modifier.width(16.dp))

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = title,
                    fontWeight = FontWeight.Bold,
                    fontSize = 16.sp,
                    color = MiuixTheme.colorScheme.onSurface // 修复：标题颜色应该始终是表面文本色
                )
                Spacer(modifier = Modifier.height(2.dp))
                Text(
                    text = description, // 使用传入的描述
                    fontSize = 13.sp,
                    color = descriptionColor // 使用传入的具体颜色判定
                )
            }

            // Status label (Pill shape)
            Box(
                modifier = Modifier
                    .background(
                        color = statusColor.copy(alpha = 0.2f),
                        shape = RoundedCornerShape(50)
                    )
                    .padding(horizontal = 8.dp, vertical = 4.dp)
            ) {
                Text(
                    text = if (isEnabled) stringResource(R.string.status_on) else stringResource(R.string.status_off),
                    color = statusColor, // Pill 标签文字跟随状态
                    fontWeight = FontWeight.Bold,
                    fontSize = 11.sp
                )
            }
        }
    }
}
