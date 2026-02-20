package com.merak.ui.page.settings

import android.Manifest
import android.content.pm.PackageManager
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.tween
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import androidx.navigation.NavController
import com.merak.service.ThemeInstallAccessibilityService
import com.merak.ui.Route
import com.merak.ui.components.MiuixNavigationItemWidget
import com.merak.ui.theme.getMiuixAppBarColor
import com.merak.ui.theme.rememberMiuixHazeStyle
import com.merak.ui.theme.tsHazeEffect
import com.merak.util.toast
import com.merak.x.R
import dev.chrisbanes.haze.HazeState
import dev.chrisbanes.haze.hazeSource
import org.koin.androidx.compose.koinViewModel
import top.yukonga.miuix.kmp.basic.Card
import top.yukonga.miuix.kmp.basic.MiuixScrollBehavior
import top.yukonga.miuix.kmp.basic.Scaffold
import top.yukonga.miuix.kmp.basic.SmallTitle
import top.yukonga.miuix.kmp.basic.TopAppBar
import top.yukonga.miuix.kmp.basic.rememberTopAppBarState
import top.yukonga.miuix.kmp.extra.SuperArrow
import top.yukonga.miuix.kmp.extra.SuperSwitch
import top.yukonga.miuix.kmp.utils.overScrollVertical
import top.yukonga.miuix.kmp.utils.scrollEndHaptic

@Composable
fun SettingsPage(
    navController: NavController,
    viewModel: SettingsViewModel = koinViewModel(),
    hazeState: HazeState?,
    outerPadding: PaddingValues = PaddingValues(0.dp)
) {
    val context = LocalContext.current
    val settingsState by viewModel.uiState.collectAsState()

    val scrollBehavior = MiuixScrollBehavior(rememberTopAppBarState())
    val hazeStyle = rememberMiuixHazeStyle()

    val showOptimizationDialog = remember { mutableStateOf(false) }

    fun hasNotificationPermission() =
        ContextCompat.checkSelfPermission(
            context,
            Manifest.permission.POST_NOTIFICATIONS
        ) == PackageManager.PERMISSION_GRANTED

    fun isAccessibilityEnabled(): Boolean =
        ThemeInstallAccessibilityService.isAccessibilityServiceEnabled(
            context,
            ThemeInstallAccessibilityService::class.java
        )

    if (settingsState == null) return
    val settings = settingsState!!

    Scaffold(
        topBar = {
            TopAppBar(
                modifier = Modifier.tsHazeEffect(hazeState, hazeStyle),
                color = hazeState.getMiuixAppBarColor(),
                title = stringResource(R.string.title_settings),
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
            // --- 外观设置 ---
            item {
                SmallTitle(stringResource(R.string.personalization))
            }
            item {
                Card(
                    modifier = Modifier
                        .padding(horizontal = 12.dp)
                        .padding(bottom = 6.dp)
                ) {
                    MiuixNavigationItemWidget(
                        title = stringResource(R.string.theme_settings),
                        description = stringResource(R.string.theme_settings_desc),
                        onClick = {
                            navController.navigate(Route.APPEARANCE)
                        }
                    )
                }
            }

            // --- 功能设置 ---
            item { SmallTitle(stringResource(R.string.home_tools_title)) }

            // 1. 常驻通知保活
            item {
                Card(
                    modifier = Modifier
                        .padding(horizontal = 12.dp)
                        .padding(bottom = 6.dp)
                ) {
                    SuperSwitch(
                        title = stringResource(R.string.keep_alive_title),
                        summary = stringResource(R.string.keep_alive_summary),
                        checked = settings.isKeepAliveEnabled,
                        onCheckedChange = { checked ->
                            if (checked) {
                                if (hasNotificationPermission()) {
                                    viewModel.toggleKeepAlive(true)
                                } else {
                                    context.toast(R.string.keep_alive_notification_permission_required)
                                }
                            } else {
                                viewModel.toggleKeepAlive(false)
                            }
                        }
                    )
                    AnimatedVisibility(
                        visible = settings.isKeepAliveEnabled,
                        enter = fadeIn(animationSpec = tween(durationMillis = 300, easing = FastOutSlowInEasing)) +
                                expandVertically(animationSpec = tween(durationMillis = 400, easing = FastOutSlowInEasing)),
                        exit = fadeOut(animationSpec = tween(durationMillis = 250, easing = FastOutSlowInEasing)) +
                                shrinkVertically(animationSpec = tween(durationMillis = 350, easing = FastOutSlowInEasing))
                    ) {
                        SuperSwitch(
                            title = stringResource(R.string.optimization_mode_title),
                            summary = stringResource(R.string.optimization_mode_summary),
                            checked = settings.isOptimizationModeEnabled,
                            onCheckedChange = { checked ->
                                if (checked) {
                                    // 开启前检查无障碍
                                    if (!isAccessibilityEnabled()) {
                                        context.toast(R.string.optimization_mode_require_accessibility)
                                    } else {
                                        showOptimizationDialog.value = true
                                    }
                                } else {
                                    // 直接关闭
                                    context.toast(R.string.optimization_mode_disabled_toast)
                                }
                            }
                        )
                    }
                }
            }

            // --- 其他 ---
            item {
                SmallTitle(stringResource(R.string.about_others))
            }
            item {
                Card(
                    modifier = Modifier
                        .padding(horizontal = 12.dp)
                        .padding(bottom = 6.dp)
                ) {
                    SuperArrow(
                        title = stringResource(R.string.about_title),
                        summary = stringResource(R.string.about_version_label), // 如果需要版本号，可以用 BuildConfig.VERSION_NAME
                        onClick = { navController.navigate(Route.ABOUT) }
                    )
                }
            }
        }
    }
}