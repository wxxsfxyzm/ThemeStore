package com.merak.ui.activity

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.merak.data.settings.repo.SettingsRepo
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class MainViewModel(
    private val settingsRepo: SettingsRepo
) : ViewModel() {
    val uiState: StateFlow<MainUiState> = settingsRepo.appSettings
        .map { it.toMainUiState() }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.Eagerly,
            initialValue = MainUiState(isLoaded = false)
        )

    fun completeOnboarding() {
        viewModelScope.launch {
            settingsRepo.setOnboardingCompleted(true)
        }
    }

    fun saveWelcomeProgress(pageIndex: Int) {
        viewModelScope.launch {
            settingsRepo.setWelcomePageIndex(pageIndex)
        }
    }
}