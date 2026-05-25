package com.merak.ui

import androidx.navigation3.runtime.NavKey
import kotlinx.serialization.Serializable

sealed interface Route : NavKey {
    @Serializable
    data object Welcome : Route

    @Serializable
    data object Main : Route

    @Serializable
    data object ThemeInstall : Route

    @Serializable
    data object FilePicker : Route

    @Serializable
    data object Log : Route

    @Serializable
    data object Appearance : Route

    @Serializable
    data object About : Route
}
