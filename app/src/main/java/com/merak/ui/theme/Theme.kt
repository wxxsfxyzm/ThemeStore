package com.merak.ui.theme

import android.os.Build
import androidx.activity.ComponentActivity
import androidx.activity.compose.LocalActivity
import androidx.annotation.RequiresApi
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.remember
import androidx.compose.runtime.staticCompositionLocalOf
import com.merak.ui.theme.adaptive.LocalWindowLayoutInfo
import com.merak.ui.theme.adaptive.rememberWindowLayoutInfo
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.res.colorResource
import androidx.core.view.WindowCompat
import com.merak.ui.theme.material.PaletteStyle
import com.merak.ui.theme.material.ThemeColorSpec
import com.merak.ui.theme.material.ThemeMode
import top.yukonga.miuix.kmp.theme.ColorSchemeMode
import top.yukonga.miuix.kmp.theme.MiuixTheme
import top.yukonga.miuix.kmp.theme.ThemeController
import top.yukonga.miuix.kmp.theme.ThemeColorSpec as MiuixColorSpec
import top.yukonga.miuix.kmp.theme.ThemePaletteStyle as MiuixPaletteStyle

private val LocalIsDark = staticCompositionLocalOf { false }

object ThemeStoreTheme {
    val isDark: Boolean
        @Composable
        get() = LocalIsDark.current
}

@Composable
fun ThemeStoreMiuixTheme(
    themeMode: ThemeMode,
    useMiuixMonet: Boolean,
    useDynamicColor: Boolean = false,
    compatStatusBarColor: Boolean = true,
    seedColor: Color,
    paletteStyle: PaletteStyle = PaletteStyle.TonalSpot,
    colorSpec: ThemeColorSpec = ThemeColorSpec.SPEC_2025,
    content: @Composable () -> Unit
) {
    // Resolve the actual dark mode state based on user settings and system state
    val isDark = when (themeMode) {
        ThemeMode.LIGHT -> false
        ThemeMode.DARK -> true
        ThemeMode.SYSTEM -> isSystemInDarkTheme()
    }

    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
        NavigationBarContrastHandler()
    }

    // Handle status bar icon colors based on the theme
    if (compatStatusBarColor) {
        val view = LocalView.current
        if (!view.isInEditMode) {
            SideEffect {
                val window = (view.context as ComponentActivity).window
                WindowCompat.getInsetsController(window, view).isAppearanceLightStatusBars = !isDark
            }
        }
    }

    // Initialize Miuix ThemeController
    val controller = if (useMiuixMonet) {
        // Monet Engine Path
        val keyColor = if (useDynamicColor && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S)
            colorResource(id = android.R.color.system_accent1_500)
        else seedColor

        val colorSchemeMode = when (themeMode) {
            ThemeMode.SYSTEM -> ColorSchemeMode.MonetSystem
            ThemeMode.LIGHT -> ColorSchemeMode.MonetLight
            ThemeMode.DARK -> ColorSchemeMode.MonetDark
        }

        val style = when (paletteStyle) {
            PaletteStyle.TonalSpot -> MiuixPaletteStyle.TonalSpot
            PaletteStyle.Neutral -> MiuixPaletteStyle.Neutral
            PaletteStyle.Vibrant -> MiuixPaletteStyle.Vibrant
            PaletteStyle.Expressive -> MiuixPaletteStyle.Expressive
            PaletteStyle.Rainbow -> MiuixPaletteStyle.Rainbow
            PaletteStyle.FruitSalad -> MiuixPaletteStyle.FruitSalad
            PaletteStyle.Monochrome -> MiuixPaletteStyle.Monochrome
            PaletteStyle.Fidelity -> MiuixPaletteStyle.Fidelity
            PaletteStyle.Content -> MiuixPaletteStyle.Content
        }

        val colorSpecVersion = when (colorSpec) {
            ThemeColorSpec.SPEC_2025 -> if (paletteStyle.supportsSpec2025) MiuixColorSpec.Spec2025 else MiuixColorSpec.Spec2021
            ThemeColorSpec.SPEC_2021 -> MiuixColorSpec.Spec2021
        }

        remember(colorSchemeMode, keyColor, paletteStyle, colorSpecVersion, isDark) {
            ThemeController(
                colorSchemeMode = colorSchemeMode,
                keyColor = keyColor,
                paletteStyle = style,
                colorSpec = colorSpecVersion,
                isDark = isDark
            )
        }
    } else {
        // Default Miuix Theme Path
        val colorSchemeMode = when (themeMode) {
            ThemeMode.SYSTEM -> ColorSchemeMode.System
            ThemeMode.LIGHT -> ColorSchemeMode.Light
            ThemeMode.DARK -> ColorSchemeMode.Dark
        }

        remember(colorSchemeMode, isDark) {
            ThemeController(
                colorSchemeMode = colorSchemeMode,
                isDark = isDark
            )
        }
    }

    // Select the appropriate semantic color palette
    val semanticColors = if (isDark) DarkSemanticColors else LightSemanticColors

    val windowLayoutInfo = rememberWindowLayoutInfo()

    // Provide the semantic colors down the Compose tree
    CompositionLocalProvider(
        LocalIsDark provides isDark,
        LocalSemanticColors provides semanticColors,
        LocalWindowLayoutInfo provides windowLayoutInfo
    ) {
        MiuixTheme(
            controller = controller,
            content = content
        )
    }
}

@RequiresApi(Build.VERSION_CODES.Q)
@Composable
private fun NavigationBarContrastHandler() {
    val configuration = LocalConfiguration.current
    val activity = LocalActivity.current

    DisposableEffect(configuration) {
        val window = activity?.window
        window?.isNavigationBarContrastEnforced = false
        onDispose {}
    }
}
