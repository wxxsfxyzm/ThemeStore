package com.merak.ui.page.accessibility

import android.app.Application
import android.content.ComponentName
import android.content.Intent
import android.content.pm.PackageManager
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.merak.core.accessibility.AccessibilityServiceDetail
import com.merak.core.accessibility.AccessibilityServiceManager
import com.merak.core.accessibility.ManagedAccessibilityService
import com.merak.core.os.shizuku.PrivilegedManager
import com.merak.data.settings.repo.SettingsRepo
import com.merak.service.AccessibilityDaemonService
import com.merak.x.R
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import rikka.shizuku.Shizuku
import timber.log.Timber

sealed interface AccessibilitySideEffect {
    data class RequestShizukuPermission(val requestCode: Int) : AccessibilitySideEffect
    data class ShowToastRes(val resId: Int) : AccessibilitySideEffect
    data class OpenIntent(val intent: Intent) : AccessibilitySideEffect
}

data class AccessibilityUiState(
    val isLoading: Boolean = true,
    val isShizukuAvailable: Boolean = false,
    val isShizukuGranted: Boolean = false,
    val hasSecureSettingsPermission: Boolean = false,
    val services: List<ManagedAccessibilityService> = emptyList(),
    val selectedDetail: AccessibilityServiceDetail? = null,
    val daemonBootEnabled: Boolean = true,
    val daemonToastEnabled: Boolean = true
) {
    val canManage: Boolean
        get() = isShizukuAvailable && isShizukuGranted && hasSecureSettingsPermission
}

class AccessibilityViewModel(
    private val context: Application,
    private val serviceManager: AccessibilityServiceManager,
    private val privilegedManager: PrivilegedManager,
    private val settingsRepo: SettingsRepo
) : ViewModel() {
    private val _uiState = MutableStateFlow(AccessibilityUiState())
    val uiState = _uiState.asStateFlow()

    private val _effect = Channel<AccessibilitySideEffect>(Channel.BUFFERED)
    val effect = _effect.receiveAsFlow()
    private var isGrantingSecurePermission = false

    private val shizukuListener = Shizuku.OnRequestPermissionResultListener { requestCode, grantResult ->
        if (requestCode == REQUEST_CODE && grantResult == PackageManager.PERMISSION_GRANTED) {
            ensurePrivilegedPermissionAndRefresh()
        } else {
            refresh()
        }
    }

    init {
        runCatching { Shizuku.addRequestPermissionResultListener(shizukuListener) }
        refresh()
    }

    override fun onCleared() {
        runCatching { Shizuku.removeRequestPermissionResultListener(shizukuListener) }
        super.onCleared()
    }

    fun refresh() {
        viewModelScope.launch(Dispatchers.IO) {
            val shizukuAvailable = runCatching { Shizuku.pingBinder() }.getOrDefault(false)
            val shizukuGranted = shizukuAvailable && runCatching {
                Shizuku.checkSelfPermission() == PackageManager.PERMISSION_GRANTED
            }.getOrDefault(false)

            if (shizukuGranted && !serviceManager.hasSecureSettingsPermission() && !isGrantingSecurePermission) {
                ensurePrivilegedPermissionAndRefresh()
                return@launch
            }

            loadState(shizukuAvailable, shizukuGranted)
        }
    }

    fun requestPermission() {
        viewModelScope.launch {
            val state = _uiState.value
            when {
                !state.isShizukuAvailable -> {
                    _effect.send(AccessibilitySideEffect.ShowToastRes(R.string.shizuku_not_running))
                }

                !state.isShizukuGranted -> {
                    _effect.send(AccessibilitySideEffect.RequestShizukuPermission(REQUEST_CODE))
                }

                !state.hasSecureSettingsPermission -> {
                    ensurePrivilegedPermissionAndRefresh()
                }
            }
        }
    }

    fun setServiceEnabled(service: ManagedAccessibilityService, enabled: Boolean) {
        if (!_uiState.value.canManage) return
        viewModelScope.launch(Dispatchers.IO) {
            if (!serviceManager.setServiceEnabled(service.componentName, enabled)) {
                _effect.send(AccessibilitySideEffect.ShowToastRes(R.string.accessibility_operation_failed))
            }
            delay(300L)
            refresh()
        }
    }

    fun togglePinned(service: ManagedAccessibilityService) {
        viewModelScope.launch(Dispatchers.IO) {
            val settings = settingsRepo.appSettings.first()
            val next = settings.accessibilityPinnedServices.toMutableList()
            if (next.remove(service.id).not()) {
                next.add(0, service.id)
            }
            settingsRepo.setAccessibilityPinnedServices(next)
            refresh()
        }
    }

    fun setDaemonEnabled(service: ManagedAccessibilityService, enabled: Boolean) {
        viewModelScope.launch(Dispatchers.IO) {
            if (enabled && !AccessibilityDaemonService.hasNotificationPermission(context)) {
                _effect.send(AccessibilitySideEffect.ShowToastRes(R.string.keep_alive_notification_permission_required))
                return@launch
            }

            val settings = settingsRepo.appSettings.first()
            val next = settings.accessibilityDaemonServices.toMutableSet()
            if (enabled) next.add(service.id) else next.remove(service.id)
            settingsRepo.setAccessibilityDaemonServices(next)

            if (next.isEmpty()) {
                AccessibilityDaemonService.stop(context)
            } else {
                AccessibilityDaemonService.start(context)
            }
            refresh()
        }
    }

    fun setDaemonBootEnabled(enabled: Boolean) {
        viewModelScope.launch(Dispatchers.IO) {
            settingsRepo.setAccessibilityDaemonBootEnabled(enabled)
            refresh()
        }
    }

    fun setDaemonToastEnabled(enabled: Boolean) {
        viewModelScope.launch(Dispatchers.IO) {
            settingsRepo.setAccessibilityDaemonToastEnabled(enabled)
            refresh()
        }
    }

    fun showDetail(service: ManagedAccessibilityService) {
        _uiState.update { it.copy(selectedDetail = serviceManager.buildDetail(service.info)) }
    }

    fun dismissDetail() {
        _uiState.update { it.copy(selectedDetail = null) }
    }

    fun openSettings(componentName: ComponentName) {
        viewModelScope.launch {
            _effect.send(AccessibilitySideEffect.OpenIntent(serviceManager.settingsIntent(componentName)))
        }
    }

    private fun ensurePrivilegedPermissionAndRefresh() {
        if (isGrantingSecurePermission) return
        isGrantingSecurePermission = true
        viewModelScope.launch(Dispatchers.IO) {
            try {
                privilegedManager.grantStorageAndNotificationPermissions()
            } catch (e: Throwable) {
                Timber.tag(TAG).e(e, "Failed to grant privileged permissions")
            } finally {
                delay(300L)
                isGrantingSecurePermission = false
            }
            refresh()
        }
    }

    private suspend fun loadState(shizukuAvailable: Boolean, shizukuGranted: Boolean) {
        val hasSecureSettingsPermission = serviceManager.hasSecureSettingsPermission()
        val settings = settingsRepo.appSettings.first()
        val services = if (shizukuGranted && hasSecureSettingsPermission) {
            serviceManager.loadServices(
                pinnedIds = settings.accessibilityPinnedServices,
                daemonIds = settings.accessibilityDaemonServices
            )
        } else {
            emptyList()
        }

        _uiState.update {
            it.copy(
                isLoading = false,
                isShizukuAvailable = shizukuAvailable,
                isShizukuGranted = shizukuGranted,
                hasSecureSettingsPermission = hasSecureSettingsPermission,
                services = services,
                daemonBootEnabled = settings.isAccessibilityDaemonBootEnabled,
                daemonToastEnabled = settings.isAccessibilityDaemonToastEnabled
            )
        }
    }

    private companion object {
        const val TAG = "AccessibilityViewModel"
        const val REQUEST_CODE = 2102
    }
}
