package com.merak.ui.activity

import androidx.compose.ui.graphics.Color
import com.merak.data.settings.model.AppSettings
import com.merak.ui.theme.m3color.PresetColors
import com.merak.ui.theme.m3color.ThemeMode

data class MainUiState(
    val isLoaded: Boolean = false,
    val showWelcome: Boolean = true,
    val initialWelcomePage: Int = 0,

    // --- Appearance (主题外观) ---
    val useMiuix: Boolean = false,
    val themeMode: ThemeMode = ThemeMode.SYSTEM,
    val useDynamicColor: Boolean = true,
    val useMiuixMonet: Boolean = false,
    val seedColor: Color = PresetColors.first().color,
    val useBlur: Boolean = true
)

fun AppSettings.toMainUiState(): MainUiState =
    MainUiState(
        isLoaded = true,
        showWelcome = !this.isOnboardingCompleted,
        initialWelcomePage = this.welcomePageIndex,
        themeMode = this.themeMode,
        useDynamicColor = this.useDynamicColor,
        useMiuixMonet = this.useMiuixMonet,
        seedColor = Color(this.seedColor),
        useBlur = this.useBlur
    )