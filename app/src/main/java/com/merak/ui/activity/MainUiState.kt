package com.merak.ui.activity

import androidx.compose.ui.graphics.Color
import com.merak.data.settings.model.AppSettings
import com.merak.ui.theme.material.PaletteStyle
import com.merak.ui.theme.material.PresetColors
import com.merak.ui.theme.material.ThemeColorSpec
import com.merak.ui.theme.material.ThemeMode

data class MainUiState(
    val isLoaded: Boolean = false,
    val showWelcome: Boolean = true,
    val initialWelcomePage: Int = 0,

    // --- Appearance (主题外观) ---
    val useMiuix: Boolean = false,
    val themeMode: ThemeMode = ThemeMode.SYSTEM,
    val useDynamicColor: Boolean = true,
    val useMiuixMonet: Boolean = false,
    val paletteStyle: PaletteStyle = PaletteStyle.TonalSpot,
    val colorSpec: ThemeColorSpec = ThemeColorSpec.SPEC_2025,
    val seedColor: Color = PresetColors.first().color,
    val useBlur: Boolean = true,
    val useAppleFloatingBar: Boolean = false
)

fun AppSettings.toMainUiState(): MainUiState =
    MainUiState(
        isLoaded = true,
        showWelcome = !this.isOnboardingCompleted,
        initialWelcomePage = this.welcomePageIndex,
        themeMode = this.themeMode,
        useDynamicColor = this.useDynamicColor,
        useMiuixMonet = this.useMiuixMonet,
        paletteStyle = this.paletteStyle,
        colorSpec = this.colorSpec,
        seedColor = Color(this.seedColor),
        useBlur = this.useBlur,
        useAppleFloatingBar = this.useAppleFloatingBar
    )
