package com.merak.data.settings.repo

import com.merak.data.settings.model.AppSettings
import com.merak.ui.theme.material.PaletteStyle
import com.merak.ui.theme.material.ThemeColorSpec
import com.merak.ui.theme.material.ThemeMode
import kotlinx.coroutines.flow.Flow

interface SettingsRepo {
    val appSettings: Flow<AppSettings>

    suspend fun setOnboardingCompleted(completed: Boolean)
    suspend fun setWelcomePageIndex(index: Int)
    suspend fun setThemeMode(mode: ThemeMode)
    suspend fun setUseDynamicColor(use: Boolean)
    suspend fun setUseBlur(use: Boolean)
    suspend fun setUseAppleFloatingBar(use: Boolean)
    suspend fun setUseMiuixMonet(use: Boolean)
    suspend fun setPaletteStyle(style: PaletteStyle)
    suspend fun setColorSpec(spec: ThemeColorSpec)
    suspend fun setSeedColor(colorInt: Int)

    suspend fun setKeepAliveEnabled(enabled: Boolean)
}
