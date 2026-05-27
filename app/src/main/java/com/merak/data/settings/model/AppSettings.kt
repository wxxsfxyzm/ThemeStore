package com.merak.data.settings.model

import com.merak.ui.theme.material.PaletteStyle
import com.merak.ui.theme.material.ThemeColorSpec
import com.merak.ui.theme.material.ThemeMode

data class AppSettings(
    val isOnboardingCompleted: Boolean,
    val welcomePageIndex: Int,
    val isShizukuPrivilegedBootstrapCompleted: Boolean = false,

    // UI
    val themeMode: ThemeMode,
    val useDynamicColor: Boolean,
    val useMiuixMonet: Boolean,
    val paletteStyle: PaletteStyle,
    val colorSpec: ThemeColorSpec,
    val seedColor: Int,
    val useBlur: Boolean,
    val useAppleFloatingBar: Boolean,

    // Settings
    val isKeepAliveEnabled: Boolean = false,
    val isOptimizationModeEnabled: Boolean = false,
    val accessibilityPinnedServices: List<String> = emptyList(),
    val accessibilityDaemonServices: Set<String> = emptySet(),
    val isAccessibilityDaemonBootEnabled: Boolean = true,
    val isAccessibilityDaemonToastEnabled: Boolean = true,
)
