package com.merak.data.settings.model

import com.merak.ui.theme.m3color.ThemeMode

data class AppSettings(
    val isOnboardingCompleted: Boolean,
    val welcomePageIndex: Int,

    // UI
    val themeMode: ThemeMode,
    val useDynamicColor: Boolean,
    val useMiuixMonet: Boolean,
    val seedColor: Int,
    val useBlur: Boolean,

    // Settings
    val isKeepAliveEnabled: Boolean = false,
    val isOptimizationModeEnabled: Boolean = false,
)