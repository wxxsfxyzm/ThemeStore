package com.merak.ui.theme.m3color

import androidx.compose.material3.ColorScheme
import androidx.compose.runtime.Stable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import com.kyant.m3color.hct.Hct
import com.kyant.m3color.scheme.SchemeTonalSpot

@Stable
fun dynamicColorScheme(
    keyColor: Color,
    isDark: Boolean,
    contrastLevel: Double = 0.0
): ColorScheme {
    val hct = Hct.fromInt(keyColor.toArgb())
    val scheme = SchemeTonalSpot(hct, isDark, contrastLevel)

    return ColorScheme(
        primary = scheme.primary.toColor(),
        onPrimary = scheme.onPrimary.toColor(),
        primaryContainer = scheme.primaryContainer.toColor(),
        onPrimaryContainer = scheme.onPrimaryContainer.toColor(),
        inversePrimary = scheme.inversePrimary.toColor(),
        secondary = scheme.secondary.toColor(),
        onSecondary = scheme.onSecondary.toColor(),
        secondaryContainer = scheme.secondaryContainer.toColor(),
        onSecondaryContainer = scheme.onSecondaryContainer.toColor(),
        tertiary = scheme.tertiary.toColor(),
        onTertiary = scheme.onTertiary.toColor(),
        tertiaryContainer = scheme.tertiaryContainer.toColor(),
        onTertiaryContainer = scheme.onTertiaryContainer.toColor(),
        background = scheme.background.toColor(),
        onBackground = scheme.onBackground.toColor(),
        surface = scheme.surface.toColor(),
        onSurface = scheme.onSurface.toColor(),
        surfaceVariant = scheme.surfaceVariant.toColor(),
        onSurfaceVariant = scheme.onSurfaceVariant.toColor(),
        surfaceTint = scheme.primary.toColor(),
        inverseSurface = scheme.inverseSurface.toColor(),
        inverseOnSurface = scheme.inverseOnSurface.toColor(),
        error = scheme.error.toColor(),
        onError = scheme.onError.toColor(),
        errorContainer = scheme.errorContainer.toColor(),
        onErrorContainer = scheme.onErrorContainer.toColor(),
        outline = scheme.outline.toColor(),
        outlineVariant = scheme.outlineVariant.toColor(),
        scrim = scheme.scrim.toColor(),
        surfaceBright = scheme.surfaceBright.toColor(),
        surfaceDim = scheme.surfaceDim.toColor(),
        surfaceContainer = scheme.surfaceContainer.toColor(),
        surfaceContainerHigh = scheme.surfaceContainerHigh.toColor(),
        surfaceContainerHighest = scheme.surfaceContainerHighest.toColor(),
        surfaceContainerLow = scheme.surfaceContainerLow.toColor(),
        surfaceContainerLowest = scheme.surfaceContainerLowest.toColor(),
    )
}

@Suppress("NOTHING_TO_INLINE")
private inline fun Int.toColor(): Color = Color(this)