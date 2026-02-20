package com.merak.ui.page.settings.theme

import androidx.compose.ui.graphics.Color
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.merak.ui.theme.m3color.PresetColors
import com.merak.data.settings.repo.SettingsRepo
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class AppearanceViewModel(
    private val settingsRepo: SettingsRepo
) : ViewModel() {

    val state: StateFlow<AppearanceState> = settingsRepo.appSettings
        .map { settings ->
            AppearanceState(
                isLoading = false,
                themeMode = settings.themeMode,
                useDynamicColor = settings.useDynamicColor,
                useMiuixMonet = settings.useMiuixMonet,
                seedColor = Color(settings.seedColor),
                availableColors = PresetColors
            )
        }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = AppearanceState(isLoading = true)
        )

    fun dispatch(action: AppearanceAction) {
        viewModelScope.launch {
            when (action) {
                is AppearanceAction.SetThemeMode -> settingsRepo.setThemeMode(action.mode)
                is AppearanceAction.SetUseDynamicColor -> settingsRepo.setUseDynamicColor(action.use)
                is AppearanceAction.SetUseMiuixMonet -> settingsRepo.setUseMiuixMonet(action.use)
                is AppearanceAction.SetSeedColor -> settingsRepo.setSeedColor(action.color.value.toInt())
            }
        }
    }
}