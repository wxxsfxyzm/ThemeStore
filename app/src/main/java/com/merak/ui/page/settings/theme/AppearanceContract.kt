package com.merak.ui.page.settings.theme

import androidx.compose.ui.graphics.Color
import com.merak.ui.theme.material.PaletteStyle
import com.merak.ui.theme.material.RawColor
import com.merak.ui.theme.material.ThemeColorSpec
import com.merak.ui.theme.material.ThemeMode

data class AppearanceState(
    val isLoading: Boolean = true,
    val themeMode: ThemeMode = ThemeMode.SYSTEM,
    val useBlur: Boolean = true,
    val useAppleFloatingBar: Boolean = false,
    val useDynamicColor: Boolean = true,
    val useMiuixMonet: Boolean = false,
    val paletteStyle: PaletteStyle = PaletteStyle.TonalSpot,
    val colorSpec: ThemeColorSpec = ThemeColorSpec.SPEC_2025,
    val seedColor: Color = Color.Blue,
    val availableColors: List<RawColor> = emptyList()
)

sealed class AppearanceAction {
    data class SetThemeMode(val mode: ThemeMode) : AppearanceAction()
    data class SetUseDynamicColor(val use: Boolean) : AppearanceAction()
    data class SetUseBlur(val use: Boolean) : AppearanceAction()
    data class SetUseAppleFloatingBar(val use: Boolean) : AppearanceAction()
    data class SetUseMiuixMonet(val use: Boolean) : AppearanceAction()
    data class SetPaletteStyle(val style: PaletteStyle) : AppearanceAction()
    data class SetColorSpec(val spec: ThemeColorSpec) : AppearanceAction()
    data class SetSeedColor(val color: Color) : AppearanceAction()
}
