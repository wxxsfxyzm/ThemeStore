package com.merak.ui.page.home

import android.content.Intent
import android.provider.Settings
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
import com.merak.ui.components.OnLifecycleEvent
import com.merak.ui.theme.getMiuixAppBarColor
import com.merak.ui.theme.rememberMiuixBlurBackdrop
import com.merak.ui.theme.tsMiuixBlurEffect
import com.merak.util.toast
import com.merak.x.R
import org.koin.androidx.compose.koinViewModel
import rikka.shizuku.Shizuku
import top.yukonga.miuix.kmp.basic.Card
import top.yukonga.miuix.kmp.basic.Icon
import top.yukonga.miuix.kmp.basic.MiuixScrollBehavior
import top.yukonga.miuix.kmp.basic.Scaffold
import top.yukonga.miuix.kmp.basic.SmallTitle
import top.yukonga.miuix.kmp.basic.Text
import top.yukonga.miuix.kmp.basic.TopAppBar
import top.yukonga.miuix.kmp.blur.layerBackdrop
import top.yukonga.miuix.kmp.preference.ArrowPreference as SuperArrow
import top.yukonga.miuix.kmp.theme.MiuixTheme
import top.yukonga.miuix.kmp.utils.overScrollVertical
import top.yukonga.miuix.kmp.utils.scrollEndHaptic

@Composable
fun HomePage(
    onNavigateToThemeInstall: () -> Unit,
    onNavigateToLog: () -> Unit,
    viewModel: HomeViewModel = koinViewModel(),
    enableBlur: Boolean,
    outerPadding: PaddingValues = PaddingValues(0.dp)
) {
    val context = LocalContext.current
    val uiState by viewModel.uiState.collectAsState()
    val scrollBehavior = MiuixScrollBehavior()

    OnLifecycleEvent(Lifecycle.Event.ON_RESUME) {
        viewModel.refreshState()
    }

    LaunchedEffect(Unit) {
        viewModel.effect.collect { effect ->
            when (effect) {
                is HomeSideEffect.OpenAccessibilitySettings -> {
                    val intent = Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS)
                    try {
                        context.startActivity(intent)
                    } catch (e: Exception) {
                        context.toast(R.string.about_open_failed)
                    }
                }

                is HomeSideEffect.ShowToast -> context.toast(effect.message)
                is HomeSideEffect.ShowToastRes -> context.toast(effect.resId)
                is HomeSideEffect.OpenShizukuManager -> {
                    val intent = context.packageManager.getLaunchIntentForPackage("moe.shizuku.privileged.api")
                    if (intent != null) context.startActivity(intent)
                    else context.toast(R.string.shizuku_not_installed)
                }

                is HomeSideEffect.RequestShizukuPermission -> {
                    try {
                        Shizuku.requestPermission(effect.requestCode)
                    } catch (e: Exception) {
                        context.toast("Error: ${e.message}")
                    }
                }
            }
        }
    }

    val blurBackdrop = rememberMiuixBlurBackdrop(enableBlur)

    Scaffold(
        topBar = {
            TopAppBar(
                modifier = Modifier.tsMiuixBlurEffect(blurBackdrop),
                color = blurBackdrop.getMiuixAppBarColor(),
                title = stringResource(R.string.title_home),
                scrollBehavior = scrollBehavior
            )
        }
    ) { innerPadding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .then(blurBackdrop?.let { Modifier.layerBackdrop(it) } ?: Modifier)
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

                val accDescription = when {
                    !isAccEnabled -> stringResource(R.string.home_accessibility_status_off)
                    isKeepAlive -> stringResource(
                        R.string.home_accessibility_status_running,
                        stringResource(R.string.home_accessibility_status_on)
                    )

                    else -> stringResource(
                        R.string.home_accessibility_status_not_running,
                        stringResource(R.string.home_accessibility_status_on)
                    )
                }

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
                        onClick = onNavigateToThemeInstall
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
                        onClick = onNavigateToLog
                    )
                }
            }
        }
    }
}

@Composable
private fun StatusCard(
    icon: Painter,
    title: String,
    isEnabled: Boolean,
    description: String,
    descriptionColor: Color,
    onClick: () -> Unit
) {
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
                .background(backgroundColor)
                .clickable(onClick = onClick)
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Icon section
            Icon(
                painter = icon,
                contentDescription = null,
                tint = statusColor,
                modifier = Modifier.size(24.dp)
            )

            Spacer(modifier = Modifier.width(16.dp))

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = title,
                    fontWeight = FontWeight.Bold,
                    fontSize = 16.sp,
                    color = MiuixTheme.colorScheme.onSurface
                )
                Spacer(modifier = Modifier.height(2.dp))
                Text(
                    text = description,
                    fontSize = 13.sp,
                    color = descriptionColor
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
                    color = statusColor,
                    fontWeight = FontWeight.Bold,
                    fontSize = 11.sp
                )
            }
        }
    }
}
