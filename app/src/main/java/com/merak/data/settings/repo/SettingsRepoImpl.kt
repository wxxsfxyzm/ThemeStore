package com.merak.data.settings.repo

import androidx.compose.ui.graphics.toArgb
import com.merak.data.settings.local.AppDataStore
import com.merak.data.settings.model.AppSettings
import com.merak.ui.theme.m3color.PresetColors
import com.merak.ui.theme.m3color.ThemeMode
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.map

class SettingsRepoImpl(
    private val dataStore: AppDataStore
) : SettingsRepo {

    override val appSettings: Flow<AppSettings> = combine(
        dataStore.getBoolean(AppDataStore.Companion.ONBOARDING_COMPLETED, false), // 默认没完成
        dataStore.getInt(AppDataStore.Companion.WELCOME_PAGE_INDEX, 0),
        // UI Settings
        dataStore.getString(AppDataStore.Companion.THEME_MODE, ThemeMode.SYSTEM.name)
            .map { runCatching { ThemeMode.valueOf(it) }.getOrDefault(ThemeMode.SYSTEM) },
        dataStore.getBoolean(AppDataStore.Companion.THEME_USE_DYNAMIC_COLOR, true),
        dataStore.getBoolean(AppDataStore.Companion.UI_USE_MIUIX_MONET, false),
        dataStore.getInt(AppDataStore.Companion.THEME_SEED_COLOR, PresetColors.first().color.toArgb()),
        dataStore.getBoolean(AppDataStore.Companion.UI_USE_BLUR, true),
        dataStore.getBoolean(AppDataStore.Companion.KEEP_ALIVE_ENABLED, false),
        dataStore.getBoolean(AppDataStore.Companion.OPTIMIZATION_MODE_ENABLED, false),
    ) { values: Array<Any?> ->
        var i = 0

        val isOnboardingCompleted = values[i++] as Boolean
        val welcomeIndex = values[i++] as Int
        val themeMode = values[i++] as ThemeMode
        val useDynamic = values[i++] as Boolean
        val useMonet = values[i++] as Boolean
        val seedColor = values[i++] as Int
        val useBlur = values[i++] as Boolean
        val keepAliveEnabled = values[i++] as Boolean
        val optimizationModeEnabled = values[i++] as Boolean

        AppSettings(
            isOnboardingCompleted = isOnboardingCompleted,
            welcomePageIndex = welcomeIndex,
            themeMode = themeMode,
            useDynamicColor = useDynamic,
            useMiuixMonet = useMonet,
            seedColor = seedColor,
            useBlur = useBlur,
            isKeepAliveEnabled = keepAliveEnabled,
            isOptimizationModeEnabled = optimizationModeEnabled
        )
    }

    override suspend fun setOnboardingCompleted(completed: Boolean) {
        dataStore.putBoolean(AppDataStore.Companion.ONBOARDING_COMPLETED, completed)
    }

    override suspend fun setWelcomePageIndex(index: Int) {
        dataStore.putInt(AppDataStore.Companion.WELCOME_PAGE_INDEX, index)
    }

    override suspend fun setThemeMode(mode: ThemeMode) {
        dataStore.putString(AppDataStore.Companion.THEME_MODE, mode.name)
    }

    override suspend fun setUseDynamicColor(use: Boolean) {
        dataStore.putBoolean(AppDataStore.Companion.THEME_USE_DYNAMIC_COLOR, use)
    }

    override suspend fun setUseMiuixMonet(use: Boolean) {
        dataStore.putBoolean(AppDataStore.Companion.UI_USE_MIUIX_MONET, use)
    }

    override suspend fun setSeedColor(colorInt: Int) {
        dataStore.putInt(AppDataStore.Companion.THEME_SEED_COLOR, colorInt)
    }

    override suspend fun setKeepAliveEnabled(enabled: Boolean) {
        dataStore.putBoolean(AppDataStore.Companion.KEEP_ALIVE_ENABLED, enabled)
    }
}