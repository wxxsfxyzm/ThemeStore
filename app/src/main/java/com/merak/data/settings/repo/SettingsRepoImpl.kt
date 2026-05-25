package com.merak.data.settings.repo

import androidx.compose.ui.graphics.toArgb
import com.merak.data.settings.local.AppDataStore
import com.merak.data.settings.model.AppSettings
import com.merak.ui.theme.material.PaletteStyle
import com.merak.ui.theme.material.PresetColors
import com.merak.ui.theme.material.ThemeColorSpec
import com.merak.ui.theme.material.ThemeMode
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.shareIn

class SettingsRepoImpl(
    private val dataStore: AppDataStore,
    appScope: CoroutineScope
) : SettingsRepo {

    override val appSettings: Flow<AppSettings> = dataStore.data
        .map { prefs ->
            AppSettings(
                isOnboardingCompleted = prefs[AppDataStore.ONBOARDING_COMPLETED] ?: false,
                welcomePageIndex = prefs[AppDataStore.WELCOME_PAGE_INDEX] ?: 0,
                themeMode = runCatching {
                    ThemeMode.valueOf(prefs[AppDataStore.THEME_MODE] ?: ThemeMode.SYSTEM.name)
                }.getOrDefault(ThemeMode.SYSTEM),
                useDynamicColor = prefs[AppDataStore.THEME_USE_DYNAMIC_COLOR] ?: true,
                useMiuixMonet = prefs[AppDataStore.UI_USE_MIUIX_MONET] ?: false,
                paletteStyle = PaletteStyle.fromValueOrDefault(
                    prefs[AppDataStore.THEME_PALETTE_STYLE] ?: PaletteStyle.TonalSpot.name
                ),
                colorSpec = ThemeColorSpec.fromValueOrDefault(
                    prefs[AppDataStore.THEME_COLOR_SPEC] ?: ThemeColorSpec.SPEC_2025.name
                ),
                seedColor = prefs[AppDataStore.THEME_SEED_COLOR] ?: PresetColors.first().color.toArgb(),
                useBlur = prefs[AppDataStore.UI_USE_BLUR] ?: true,
                useAppleFloatingBar = prefs[AppDataStore.UI_USE_APPLE_FLOATING_BAR] ?: false,
                isKeepAliveEnabled = prefs[AppDataStore.KEEP_ALIVE_ENABLED] ?: false,
                isOptimizationModeEnabled = prefs[AppDataStore.OPTIMIZATION_MODE_ENABLED] ?: false
            )
        }.shareIn(
        scope = appScope,
        started = SharingStarted.Eagerly,
        replay = 1
    )

    override suspend fun setOnboardingCompleted(completed: Boolean) {
        dataStore.putBoolean(AppDataStore.ONBOARDING_COMPLETED, completed)
    }

    override suspend fun setWelcomePageIndex(index: Int) {
        dataStore.putInt(AppDataStore.WELCOME_PAGE_INDEX, index)
    }

    override suspend fun setThemeMode(mode: ThemeMode) {
        dataStore.putString(AppDataStore.THEME_MODE, mode.name)
    }

    override suspend fun setUseDynamicColor(use: Boolean) {
        dataStore.putBoolean(AppDataStore.THEME_USE_DYNAMIC_COLOR, use)
    }

    override suspend fun setUseBlur(use: Boolean) {
        dataStore.putBoolean(AppDataStore.UI_USE_BLUR, use)
    }

    override suspend fun setUseAppleFloatingBar(use: Boolean) {
        dataStore.putBoolean(AppDataStore.UI_USE_APPLE_FLOATING_BAR, use)
    }

    override suspend fun setUseMiuixMonet(use: Boolean) {
        dataStore.putBoolean(AppDataStore.UI_USE_MIUIX_MONET, use)
    }

    override suspend fun setPaletteStyle(style: PaletteStyle) {
        dataStore.putString(AppDataStore.THEME_PALETTE_STYLE, style.name)
    }

    override suspend fun setColorSpec(spec: ThemeColorSpec) {
        dataStore.putString(AppDataStore.THEME_COLOR_SPEC, spec.name)
    }

    override suspend fun setSeedColor(colorInt: Int) {
        dataStore.putInt(AppDataStore.THEME_SEED_COLOR, colorInt)
    }

    override suspend fun setKeepAliveEnabled(enabled: Boolean) {
        dataStore.putBoolean(AppDataStore.KEEP_ALIVE_ENABLED, enabled)
    }
}
