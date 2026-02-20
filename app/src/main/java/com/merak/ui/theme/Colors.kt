package com.merak.ui.theme

import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Color

// Light mode color palette
val SuccessBgLight = Color(0xFFE8F5E9)
val SuccessTextLight = Color(0xFF2E7D32)

val WarningBgLight = Color(0xFFFFF3E0)
val WarningTextLight = Color(0xFFEF6C00)

val ErrorBgLight = Color(0xFFFFEBEE)
val ErrorTextLight = Color(0xFFC62828)

val InfoBgLight = Color(0xFFF5F5F5)
val InfoTextLight = Color(0xFF757575)
val InfoTextDarkLight = Color(0xFF424242)

val LinkTextLight = Color(0xFF1976D2)

// Dark mode color palette
val SuccessBgDark = Color(0xFF123315)
val SuccessTextDark = Color(0xFF81C784)

val WarningBgDark = Color(0xFF3E1D04)
val WarningTextDark = Color(0xFFFFB74D)

val ErrorBgDark = Color(0xFF3B0F12)
val ErrorTextDark = Color(0xFFE57373)

val InfoBgDark = Color(0xFF1E1E1E)
val InfoTextDark = Color(0xFFAAAAAA)
val InfoTextDarkDark = Color(0xFFCCCCCC)

val LinkTextDark = Color(0xFF90CAF9)

// Semantic color data class
data class SemanticColors(
    val successBg: Color,
    val successText: Color,
    val warningBg: Color,
    val warningText: Color,
    val errorBg: Color,
    val errorText: Color,
    val infoBg: Color,
    val infoText: Color,
    val infoTextDark: Color,
    val linkText: Color
)

// Pre-defined light theme colors
val LightSemanticColors = SemanticColors(
    successBg = SuccessBgLight,
    successText = SuccessTextLight,
    warningBg = WarningBgLight,
    warningText = WarningTextLight,
    errorBg = ErrorBgLight,
    errorText = ErrorTextLight,
    infoBg = InfoBgLight,
    infoText = InfoTextLight,
    infoTextDark = InfoTextDarkLight,
    linkText = LinkTextLight
)

// Pre-defined dark theme colors
val DarkSemanticColors = SemanticColors(
    successBg = SuccessBgDark,
    successText = SuccessTextDark,
    warningBg = WarningBgDark,
    warningText = WarningTextDark,
    errorBg = ErrorBgDark,
    errorText = ErrorTextDark,
    infoBg = InfoBgDark,
    infoText = InfoTextDark,
    infoTextDark = InfoTextDarkDark,
    linkText = LinkTextDark
)

// Create CompositionLocal to provide these colors down the tree
val LocalSemanticColors = staticCompositionLocalOf { LightSemanticColors }