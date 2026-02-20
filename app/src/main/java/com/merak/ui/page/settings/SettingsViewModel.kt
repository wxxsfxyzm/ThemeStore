package com.merak.ui.page.settings

import android.app.Application
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.merak.data.settings.repo.SettingsRepo
import com.merak.service.KeepAliveService
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class SettingsViewModel(
    private val context: Application,
    private val settingsRepo: SettingsRepo
) : ViewModel() {

    val uiState = settingsRepo.appSettings
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.Eagerly,
            initialValue = null
        )

    fun toggleKeepAlive(enabled: Boolean) {
        viewModelScope.launch {
            settingsRepo.setKeepAliveEnabled(enabled)

            if (!enabled) {
                KeepAliveService.stop(context)
            } else {
                KeepAliveService.start(context)
            }
        }
    }
}