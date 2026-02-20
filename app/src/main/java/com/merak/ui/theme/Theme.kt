package com.merak.ui.theme

import androidx.activity.ComponentActivity
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.res.colorResource
import androidx.core.view.WindowCompat
import com.merak.ui.theme.m3color.ThemeMode
import top.yukonga.miuix.kmp.theme.ColorSchemeMode
import top.yukonga.miuix.kmp.theme.MiuixTheme
import top.yukonga.miuix.kmp.theme.ThemeController

@Composable
fun ThemeStoreMiuixTheme(
    themeMode: ThemeMode,
    useMiuixMonet: Boolean,
    useDynamicColor: Boolean = false,
    compatStatusBarColor: Boolean = true,
    seedColor: Color,
    content: @Composable () -> Unit
) {
    // Resolve the actual dark mode state based on user settings and system state
    val isDark = when (themeMode) {
        ThemeMode.LIGHT -> false
        ThemeMode.DARK -> true
        ThemeMode.SYSTEM -> isSystemInDarkTheme()
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
        val keyColor = if (useDynamicColor)
            colorResource(id = android.R.color.system_accent1_500)
        else seedColor

        val colorSchemeMode = when (themeMode) {
            ThemeMode.SYSTEM -> ColorSchemeMode.MonetSystem
            ThemeMode.LIGHT -> ColorSchemeMode.MonetLight
            ThemeMode.DARK -> ColorSchemeMode.MonetDark
        }

        remember(colorSchemeMode, keyColor, isDark) {
            ThemeController(
                colorSchemeMode = colorSchemeMode,
                keyColor = keyColor,
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

    // Provide the semantic colors down the Compose tree
    CompositionLocalProvider(
        LocalSemanticColors provides semanticColors
    ) {
        MiuixTheme(
            controller = controller,
            content = content
        )
    }
}