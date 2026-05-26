package com.merak.ui.page.home

import android.app.Application
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.content.pm.PackageManager
import androidx.core.content.ContextCompat
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.google.android.accessibility.selecttospeak.SelectToSpeakService
import com.merak.core.os.shizuku.AutoAccessibilityManager
import com.merak.core.os.shizuku.PrivilegedManager
import com.merak.data.settings.repo.SettingsRepo
import com.merak.service.KeepAliveService
import com.merak.service.ThemeInstallAccessibilityService
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import rikka.shizuku.Shizuku
import timber.log.Timber

// Define UI side effects
sealed interface HomeSideEffect {
    data object OpenAccessibilitySettings : HomeSideEffect
    data class ShowToast(val message: String) : HomeSideEffect
    data class ShowToastRes(val resId: Int) : HomeSideEffect
    data object OpenShizukuManager : HomeSideEffect
    data class RequestShizukuPermission(val requestCode: Int) : HomeSideEffect
}

data class HomeUiState(
    val isAccessibilityEnabled: Boolean = false,
    val isShizukuAvailable: Boolean = false,
    val isShizukuAuthorized: Boolean = false,
    val isKeepAliveRunning: Boolean = false
)

class HomeViewModel(
    private val context: Application,
    private val privilegedManager: PrivilegedManager,
    private val autoAccessibilityManager: AutoAccessibilityManager,
    private val settingsRepo: SettingsRepo
) : ViewModel() {

    private val _uiState = MutableStateFlow(HomeUiState())
    val uiState = _uiState.asStateFlow()

    private val _effect = Channel<HomeSideEffect>(Channel.BUFFERED)
    val effect = _effect.receiveAsFlow()
    private var hasGrantedPrivilegedSelfPermissions = false
    private var isPrivilegedBootstrapRunning = false
    private var accessibilityConnectionKnown = ThemeInstallAccessibilityService.isConnected()

    // Event 1: Listen for Shizuku permission grant
    private val shizukuPermissionListener = Shizuku.OnRequestPermissionResultListener { _, result ->
        if (result == PackageManager.PERMISSION_GRANTED) {
            runPrivilegedSetupAfterAuthorization()
        }
        refreshState()
    }

    // Event 2: Listen for service status
    private val serviceLifecycleReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context?, intent: Intent?) {
            when (intent?.action) {
                ThemeInstallAccessibilityService.ACTION_SERVICE_UP -> {
                    accessibilityConnectionKnown = true
                    Timber.tag(TAG).d("Accessibility broadcast: UP")
                    _uiState.update { it.copy(isAccessibilityEnabled = true) }
                    refreshState("accessibility_up_broadcast")
                }
                ThemeInstallAccessibilityService.ACTION_SERVICE_DOWN -> {
                    accessibilityConnectionKnown = false
                    Timber.tag(TAG).d("Accessibility broadcast: DOWN")
                    _uiState.update { it.copy(isAccessibilityEnabled = false) }
                    refreshState("accessibility_down_broadcast")
                }
                KeepAliveService.ACTION_STATE_CHANGED -> {
                    val isRunning = intent.getBooleanExtra(
                        KeepAliveService.EXTRA_IS_RUNNING,
                        KeepAliveService.isServiceRunning(this@HomeViewModel.context)
                    )
                    Timber.tag(TAG).d("KeepAlive broadcast: running=%s", isRunning)
                    _uiState.update { it.copy(isKeepAliveRunning = isRunning) }
                }
            }
        }
    }

    init {
        // Setup listeners
        try {
            Shizuku.addRequestPermissionResultListener(shizukuPermissionListener)
        } catch (e: Exception) {
            // Ignore
        }

        // Register filter with both UP and DOWN actions
        val filter = IntentFilter().apply {
            addAction(ThemeInstallAccessibilityService.ACTION_SERVICE_UP)
            addAction(ThemeInstallAccessibilityService.ACTION_SERVICE_DOWN)
            addAction(KeepAliveService.ACTION_STATE_CHANGED)
        }
        ContextCompat.registerReceiver(
            context,
            serviceLifecycleReceiver,
            filter,
            ContextCompat.RECEIVER_NOT_EXPORTED
        )

        // Event 3: Initialize Shizuku binder listener only when UI is active
        autoAccessibilityManager.init()

        viewModelScope.launch {
            ThemeInstallAccessibilityService.isConnectedFlow.collectLatest { isConnected ->
                accessibilityConnectionKnown = isConnected
                Timber.tag(TAG).d("Accessibility flow: connected=%s", isConnected)
                _uiState.update { it.copy(isAccessibilityEnabled = isConnected || it.isAccessibilityEnabled) }
                if (!isConnected) refreshState("accessibility_flow_disconnected")
            }
        }

        viewModelScope.launch {
            KeepAliveService.isRunningFlow.collectLatest { isRunning ->
                Timber.tag(TAG).d("KeepAlive flow: running=%s", isRunning)
                _uiState.update { it.copy(isKeepAliveRunning = isRunning) }
            }
        }

        refreshState("init")
    }

    private fun runPrivilegedSetupAfterAuthorization() {
        if (isPrivilegedBootstrapRunning) return
        isPrivilegedBootstrapRunning = true
        viewModelScope.launch(Dispatchers.IO) {
            try {
                val settings = settingsRepo.appSettings.first()
                if (settings.isShizukuPrivilegedBootstrapCompleted) {
                    grantPrivilegedSelfPermissionsIfNeeded()
                    return@launch
                }

                val completed = privilegedManager.runPrivilegedBootstrap()
                if (completed) {
                    hasGrantedPrivilegedSelfPermissions = true
                    settingsRepo.setShizukuPrivilegedBootstrapCompleted(true)
                    delay(500L)
                    refreshState("privileged_bootstrap_completed")
                } else {
                    hasGrantedPrivilegedSelfPermissions = false
                }
            } catch (e: Exception) {
                Timber.tag(TAG).e(e, "Failed to run privileged setup after Shizuku authorization")
                hasGrantedPrivilegedSelfPermissions = false
            } finally {
                isPrivilegedBootstrapRunning = false
            }
        }
    }

    private fun grantPrivilegedSelfPermissionsIfNeeded() {
        if (hasGrantedPrivilegedSelfPermissions) return
        val granted = privilegedManager.grantStorageAndNotificationPermissions()
        if (granted) {
            hasGrantedPrivilegedSelfPermissions = true
        }
    }

    private fun grantPrivilegedSelfPermissions() {
        if (hasGrantedPrivilegedSelfPermissions) return
        viewModelScope.launch(Dispatchers.IO) {
            try {
                grantPrivilegedSelfPermissionsIfNeeded()
            } catch (e: Exception) {
                Timber.tag(TAG).e(e, "Failed to grant privileged self permissions")
            }
        }
    }

    // Handle clicks on the accessibility status card
    fun toggleAccessibilityService() {
        viewModelScope.launch(Dispatchers.IO) {
            val currentState = _uiState.value

            if (currentState.isShizukuAuthorized) {
                val targetEnabled = !currentState.isAccessibilityEnabled
                try {
                    privilegedManager.setAccessibilityServiceState(targetEnabled)
                    if (!targetEnabled) {
                        accessibilityConnectionKnown = false
                        _uiState.update { it.copy(isAccessibilityEnabled = false) }
                        refreshState("toggle_disable")
                        return@launch
                    }

                    repeat(6) {
                        delay(500)
                        refreshState("toggle_poll")
                        if (_uiState.value.isAccessibilityEnabled) {
                            return@launch
                        }
                    }

                    // Fallback if polling times out
                    if (!_uiState.value.isAccessibilityEnabled) {
                        _effect.send(HomeSideEffect.ShowToast("Start timeout, please enable manually"))
                    }

                } catch (e: Exception) {
                    Timber.tag(TAG).e(e, "Failed to toggle accessibility service via Shizuku")
                    _effect.send(HomeSideEffect.ShowToast("Shizuku operation failed, please change manually"))
                    _effect.send(HomeSideEffect.OpenAccessibilitySettings)
                }
            } else {
                _effect.send(HomeSideEffect.OpenAccessibilitySettings)
            }
        }
    }

    // Handle clicks on the Shizuku status card
    fun handleShizukuCardClick() {
        viewModelScope.launch {
            val state = _uiState.value
            when {
                !state.isShizukuAvailable -> {
                    _effect.send(HomeSideEffect.OpenShizukuManager)
                }

                !state.isShizukuAuthorized -> {
                    _effect.send(HomeSideEffect.RequestShizukuPermission(1002))
                }

                else -> {
                    _effect.send(HomeSideEffect.ShowToast("Permission already granted"))
                }
            }
        }
    }

    fun refreshState(reason: String = "manual") {
        viewModelScope.launch(Dispatchers.IO) {
            val accessibilityEnabledInSystem = ThemeInstallAccessibilityService.isAccessibilityServiceEnabled(
                context,
                SelectToSpeakService::class.java
            )

            val shizukuAvailable = try {
                Shizuku.pingBinder()
            } catch (e: Exception) {
                false
            }

            val shizukuAuthorized = if (shizukuAvailable) {
                try {
                    Shizuku.checkSelfPermission() == PackageManager.PERMISSION_GRANTED
                } catch (e: Exception) {
                    false
                }
            } else {
                false
            }
            if (shizukuAuthorized) {
                runPrivilegedSetupAfterAuthorization()
            }

            val settings = settingsRepo.appSettings.first()
            var keepAliveRunning = KeepAliveService.isServiceRunning(context)
            if (settings.isKeepAliveEnabled && !keepAliveRunning) {
                KeepAliveService.start(context)
                delay(300L)
                keepAliveRunning = KeepAliveService.isServiceRunning(context)
            }

            val finalAccessibilityConnected = accessibilityConnectionKnown ||
                    ThemeInstallAccessibilityService.isConnected()
            val finalAccessibility = finalAccessibilityConnected || accessibilityEnabledInSystem

            Timber.tag(TAG).d(
                "refreshState(%s): a11yConnected=%s, a11yEnabledInSystem=%s, keepAlive=%s, shizukuAvailable=%s, shizukuAuthorized=%s",
                reason,
                finalAccessibilityConnected,
                accessibilityEnabledInSystem,
                keepAliveRunning,
                shizukuAvailable,
                shizukuAuthorized
            )

            // Update state flow to trigger UI recomposition
            _uiState.update { state ->
                state.copy(
                    isAccessibilityEnabled = finalAccessibility,
                    isShizukuAvailable = shizukuAvailable,
                    isShizukuAuthorized = shizukuAuthorized,
                    isKeepAliveRunning = keepAliveRunning
                )
            }
        }
    }

    override fun onCleared() {
        super.onCleared()
        try {
            Shizuku.removeRequestPermissionResultListener(shizukuPermissionListener)
        } catch (e: Exception) {
            // Ignore
        }
        try {
            context.unregisterReceiver(serviceLifecycleReceiver)
        } catch (e: Exception) {
            // Ignore
        }
    }

    private companion object {
        const val TAG = "HomeViewModel"
    }
}
