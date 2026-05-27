package com.merak.ui.page.accessibility

import android.widget.ImageView
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
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
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.lifecycle.Lifecycle
import com.merak.core.accessibility.AccessibilityServiceDetail
import com.merak.core.accessibility.ManagedAccessibilityService
import com.merak.ui.components.MiuixSwitchWidget
import com.merak.ui.components.OnLifecycleEvent
import com.merak.ui.theme.getMiuixAppBarColor
import com.merak.ui.theme.rememberMiuixBlurBackdrop
import com.merak.ui.theme.tsMiuixBlurEffect
import com.merak.util.toast
import com.merak.x.R
import org.koin.androidx.compose.koinViewModel
import rikka.shizuku.Shizuku
import top.yukonga.miuix.kmp.basic.ButtonDefaults
import top.yukonga.miuix.kmp.basic.Card
import top.yukonga.miuix.kmp.basic.MiuixScrollBehavior
import top.yukonga.miuix.kmp.basic.Scaffold
import top.yukonga.miuix.kmp.basic.SmallTitle
import top.yukonga.miuix.kmp.basic.Switch
import top.yukonga.miuix.kmp.basic.Text
import top.yukonga.miuix.kmp.basic.TextButton
import top.yukonga.miuix.kmp.basic.TopAppBar
import top.yukonga.miuix.kmp.blur.layerBackdrop
import top.yukonga.miuix.kmp.theme.MiuixTheme
import top.yukonga.miuix.kmp.utils.overScrollVertical
import top.yukonga.miuix.kmp.utils.scrollEndHaptic
import top.yukonga.miuix.kmp.window.WindowDialog

@Composable
fun AccessibilityPage(
    viewModel: AccessibilityViewModel = koinViewModel(),
    enableBlur: Boolean,
    outerPadding: PaddingValues = PaddingValues(0.dp)
) {
    val context = LocalContext.current
    val uiState by viewModel.uiState.collectAsState()
    val scrollBehavior = MiuixScrollBehavior()
    val blurBackdrop = rememberMiuixBlurBackdrop(enableBlur)

    OnLifecycleEvent(Lifecycle.Event.ON_RESUME) {
        viewModel.refresh()
    }

    LaunchedEffect(Unit) {
        viewModel.effect.collect { effect ->
            when (effect) {
                is AccessibilitySideEffect.RequestShizukuPermission -> {
                    runCatching { Shizuku.requestPermission(effect.requestCode) }
                        .onFailure { context.toast(it.message ?: context.getString(R.string.accessibility_operation_failed)) }
                }

                is AccessibilitySideEffect.ShowToastRes -> context.toast(effect.resId)
                is AccessibilitySideEffect.OpenIntent -> {
                    runCatching { context.startActivity(effect.intent) }
                        .onFailure { context.toast(R.string.about_open_failed) }
                }
            }
        }
    }

    uiState.selectedDetail?.let { detail ->
        AccessibilityDetailDialog(
            detail = detail,
            onOpenSettings = { detail.settingsComponent?.let(viewModel::openSettings) },
            onDismiss = viewModel::dismissDetail
        )
    }

    Scaffold(
        topBar = {
            TopAppBar(
                modifier = Modifier.tsMiuixBlurEffect(blurBackdrop),
                color = blurBackdrop.getMiuixAppBarColor(),
                title = stringResource(R.string.title_accessibility_manager),
                scrollBehavior = scrollBehavior
            )
        }
    ) { innerPadding ->
        if (!uiState.canManage) {
            PermissionRequiredContent(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(top = innerPadding.calculateTopPadding())
                    .padding(bottom = outerPadding.calculateBottomPadding()),
                isLoading = uiState.isLoading,
                onRequestPermission = viewModel::requestPermission
            )
            return@Scaffold
        }

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
            item { SmallTitle(stringResource(R.string.accessibility_daemon_settings_title)) }
            item {
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 12.dp)
                        .padding(bottom = 6.dp)
                ) {
                    MiuixSwitchWidget(
                        title = stringResource(R.string.accessibility_daemon_boot),
                        description = stringResource(R.string.accessibility_daemon_boot_summary),
                        checked = uiState.daemonBootEnabled,
                        onCheckedChange = viewModel::setDaemonBootEnabled
                    )
                    MiuixSwitchWidget(
                        title = stringResource(R.string.accessibility_daemon_toast),
                        description = stringResource(R.string.accessibility_daemon_toast_summary),
                        checked = uiState.daemonToastEnabled,
                        onCheckedChange = viewModel::setDaemonToastEnabled
                    )
                }
            }
            item { SmallTitle(stringResource(R.string.accessibility_service_list_title)) }
            if (uiState.services.isEmpty()) {
                item {
                    Text(
                        text = stringResource(R.string.accessibility_empty),
                        modifier = Modifier.padding(24.dp),
                        color = MiuixTheme.colorScheme.onSurfaceVariantSummary
                    )
                }
            } else {
                items(uiState.services, key = { it.id }) { service ->
                    AccessibilityServiceRow(
                        service = service,
                        onCheckedChange = { viewModel.setServiceEnabled(service, it) },
                        onDaemonChange = { viewModel.setDaemonEnabled(service, it) },
                        onPin = { viewModel.togglePinned(service) },
                        onClick = { viewModel.showDetail(service) }
                    )
                }
            }
        }
    }
}

@Composable
private fun PermissionRequiredContent(
    modifier: Modifier,
    isLoading: Boolean,
    onRequestPermission: () -> Unit
) {
    Box(modifier = modifier, contentAlignment = Alignment.Center) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text(
                text = if (isLoading) stringResource(R.string.loading) else stringResource(R.string.accessibility_permission_required),
                fontSize = 16.sp,
                color = MiuixTheme.colorScheme.onSurface
            )
            Spacer(modifier = Modifier.height(16.dp))
            TextButton(
                text = stringResource(R.string.shizuku_authorize),
                onClick = onRequestPermission,
                colors = ButtonDefaults.textButtonColorsPrimary()
            )
        }
    }
}

@Composable
private fun AccessibilityServiceRow(
    service: ManagedAccessibilityService,
    onCheckedChange: (Boolean) -> Unit,
    onDaemonChange: (Boolean) -> Unit,
    onPin: () -> Unit,
    onClick: () -> Unit
) {
    val statusColor = if (service.isEnabled) Color(0xFF2E7D32) else Color(0xFFD32F2F)
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 12.dp)
            .padding(bottom = 6.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .then(if (service.isPinned) Modifier.background(MiuixTheme.colorScheme.primary.copy(alpha = 0.08f)) else Modifier)
                .clickable(onClick = onClick)
                .padding(14.dp)
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                AndroidView(
                    factory = { ImageView(it).apply { scaleType = ImageView.ScaleType.CENTER_INSIDE } },
                    update = { it.setImageDrawable(service.icon) },
                    modifier = Modifier.size(42.dp)
                )
                Spacer(modifier = Modifier.width(12.dp))
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = if (service.packageLabel == service.serviceLabel) {
                            service.serviceLabel
                        } else {
                            "${service.packageLabel}/${service.serviceLabel}"
                        },
                        fontWeight = FontWeight.SemiBold,
                        fontSize = 16.sp,
                        color = MiuixTheme.colorScheme.onSurface
                    )
                    Text(
                        text = service.description.ifBlank { stringResource(R.string.accessibility_no_description) },
                        fontSize = 13.sp,
                        color = MiuixTheme.colorScheme.onSurfaceVariantSummary,
                        maxLines = 2
                    )
                    Text(
                        text = if (service.isEnabled) stringResource(R.string.status_on) else stringResource(R.string.status_off),
                        fontSize = 12.sp,
                        color = statusColor
                    )
                }
                Switch(checked = service.isEnabled, onCheckedChange = onCheckedChange)
            }

            Spacer(modifier = Modifier.height(8.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                TextButton(
                    text = if (service.isDaemonEnabled) {
                        stringResource(R.string.accessibility_daemon_enabled)
                    } else {
                        stringResource(R.string.accessibility_daemon_disabled)
                    },
                    onClick = { onDaemonChange(!service.isDaemonEnabled) }
                )
                TextButton(
                    text = if (service.isPinned) {
                        stringResource(R.string.accessibility_unpin)
                    } else {
                        stringResource(R.string.accessibility_pin)
                    },
                    onClick = onPin
                )
            }
        }
    }
}

@Composable
private fun AccessibilityDetailDialog(
    detail: AccessibilityServiceDetail,
    onOpenSettings: () -> Unit,
    onDismiss: () -> Unit
) {
    WindowDialog(
        title = stringResource(R.string.accessibility_detail_title),
        show = true,
        onDismissRequest = onDismiss
    ) {
        Column {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(360.dp)
                    .verticalScroll(rememberScrollState())
            ) {
                DetailBlock(stringResource(R.string.accessibility_detail_class), detail.serviceClass)
                DetailBlock(stringResource(R.string.accessibility_detail_capabilities), detail.capabilities)
                DetailBlock(stringResource(R.string.accessibility_detail_scope), detail.packageScope)
                DetailBlock(stringResource(R.string.accessibility_detail_feedback), detail.feedbackTypes)
                DetailBlock(stringResource(R.string.accessibility_detail_events), detail.eventTypes)
                DetailBlock(stringResource(R.string.accessibility_detail_flags), detail.flags)
            }
            Spacer(modifier = Modifier.height(12.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                TextButton(
                    text = stringResource(R.string.close),
                    onClick = onDismiss,
                    modifier = Modifier.weight(1f)
                )
                if (detail.settingsComponent != null) {
                    TextButton(
                        text = stringResource(R.string.title_settings),
                        onClick = onOpenSettings,
                        modifier = Modifier.weight(1f),
                        colors = ButtonDefaults.textButtonColorsPrimary()
                    )
                }
            }
        }
    }
}

@Composable
private fun DetailBlock(title: String, value: String) {
    Text(
        text = title,
        fontWeight = FontWeight.SemiBold,
        fontSize = 14.sp,
        color = MiuixTheme.colorScheme.onSurface,
        modifier = Modifier.padding(top = 8.dp)
    )
    Text(
        text = value,
        fontSize = 13.sp,
        color = MiuixTheme.colorScheme.onSurfaceVariantSummary
    )
}
