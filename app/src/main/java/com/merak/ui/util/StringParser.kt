package com.merak.ui.util

import androidx.compose.runtime.Composable
import androidx.compose.ui.res.stringResource
import com.merak.ui.theme.m3color.RawColor
import com.merak.x.R


@Composable
fun RawColor.getDisplayName() = when (key) {
    "default" -> stringResource(R.string.color_default)
    "pink" -> stringResource(R.string.color_pink)
    "red" -> stringResource(R.string.color_red)
    "orange" -> stringResource(R.string.color_orange)
    "amber" -> stringResource(R.string.color_amber)
    "yellow" -> stringResource(R.string.color_yellow)
    "lime" -> stringResource(R.string.color_lime)
    "green" -> stringResource(R.string.color_green)
    "cyan" -> stringResource(R.string.color_cyan)
    "teal" -> stringResource(R.string.color_teal)
    "light_blue" -> stringResource(R.string.color_light_blue)
    "blue" -> stringResource(R.string.color_blue)
    "indigo" -> stringResource(R.string.color_indigo)
    "purple" -> stringResource(R.string.color_purple)
    "deep_purple" -> stringResource(R.string.color_deep_purple)
    "blue_grey" -> stringResource(R.string.color_blue_grey)
    "brown" -> stringResource(R.string.color_brown)
    "grey" -> stringResource(R.string.color_grey)
    else -> key
}
