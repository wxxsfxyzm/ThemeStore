package com.merak.ui.page.settings

import android.Manifest
import android.content.pm.PackageManager
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
import com.google.android.accessibility.selecttospeak.SelectToSpeakService
import com.merak.service.ThemeInstallAccessibilityService
import com.merak.ui.components.MiuixNavigationItemWidget
import com.merak.ui.theme.getMiuixAppBarColor
import com.merak.ui.theme.rememberMiuixBlurBackdrop
import com.merak.ui.theme.tsMiuixBlurEffect
import com.merak.util.toast
import com.merak.x.R
import org.koin.androidx.compose.koinViewModel
import top.yukonga.miuix.kmp.basic.Card
import top.yukonga.miuix.kmp.basic.MiuixScrollBehavior
import top.yukonga.miuix.kmp.basic.Scaffold
import top.yukonga.miuix.kmp.basic.SmallTitle
import top.yukonga.miuix.kmp.basic.TopAppBar
import top.yukonga.miuix.kmp.basic.rememberTopAppBarState
import top.yukonga.miuix.kmp.blur.layerBackdrop
import top.yukonga.miuix.kmp.preference.ArrowPreference as SuperArrow
import top.yukonga.miuix.kmp.preference.SwitchPreference as SuperSwitch
import top.yukonga.miuix.kmp.utils.overScrollVertical
import top.yukonga.miuix.kmp.utils.scrollEndHaptic

@Composable
fun SettingsPage(
    onNavigateToAppearance: () -> Unit,
    onNavigateToAbout: () -> Unit,
    viewModel: SettingsViewModel = koinViewModel(),
    enableBlur: Boolean,
    outerPadding: PaddingValues = PaddingValues(0.dp)
) {
    val context = LocalContext.current
    val settingsState by viewModel.uiState.collectAsState()

    val scrollBehavior = MiuixScrollBehavior(rememberTopAppBarState())

    val showOptimizationDialog = remember { mutableStateOf(false) }

    fun hasNotificationPermission() =
        ContextCompat.checkSelfPermission(
            context,
            Manifest.permission.POST_NOTIFICATIONS
        ) == PackageManager.PERMISSION_GRANTED

    fun isAccessibilityEnabled(): Boolean =
        ThemeInstallAccessibilityService.isAccessibilityServiceEnabled(
            context,
            SelectToSpeakService::class.java
        )

    if (settingsState == null) return
    val settings = settingsState!!
    val blurBackdrop = rememberMiuixBlurBackdrop(enableBlur)

    Scaffold(
        topBar = {
            TopAppBar(
                modifier = Modifier.tsMiuixBlurEffect(blurBackdrop),
                color = blurBackdrop.getMiuixAppBarColor(),
                title = stringResource(R.string.title_settings),
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
                        onClick = onNavigateToAppearance
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
                        onClick = onNavigateToAbout
                    )
                }
            }
        }
    }
}
