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
import com.merak.core.os.shizuku.AutoAccessibilityManager
import com.merak.core.os.shizuku.PrivilegedManager
import com.merak.service.KeepAliveService
import com.merak.service.ThemeInstallAccessibilityService
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import rikka.shizuku.Shizuku

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
    private val autoAccessibilityManager: AutoAccessibilityManager // Injected manager
) : ViewModel() {

    private val _uiState = MutableStateFlow(HomeUiState())
    val uiState = _uiState.asStateFlow()

    private val _effect = Channel<HomeSideEffect>(Channel.BUFFERED)
    val effect = _effect.receiveAsFlow()

    // Event 1: Listen for Shizuku permission grant
    private val shizukuPermissionListener = Shizuku.OnRequestPermissionResultListener { _, result ->
        if (result == PackageManager.PERMISSION_GRANTED) {
            // "Listen" to the permission event: trigger auto-enable immediately
            autoAccessibilityManager.runCheck()
        }
        refreshState()
    }

    // Event 2: Listen for service status (Broadcast from the service itself)
    // Update receiver to handle both UP and DOWN events
    private val serviceLifecycleReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context?, intent: Intent?) {
            when (intent?.action) {
                ThemeInstallAccessibilityService.ACTION_SERVICE_UP,
                ThemeInstallAccessibilityService.ACTION_SERVICE_DOWN -> {
                    // Refresh UI state regardless of whether the service started or stopped
                    refreshState()
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
        }
        ContextCompat.registerReceiver(
            context,
            serviceLifecycleReceiver,
            filter,
            ContextCompat.RECEIVER_NOT_EXPORTED
        )
        viewModelScope.launch {
            com.merak.service.KeepAliveService.isRunning.collect { running ->
                _uiState.update { it.copy(isKeepAliveRunning = running) }
            }
        }
        // Event 3: Initialize Shizuku binder listener only when UI is active
        autoAccessibilityManager.init()

        refreshState()
    }

    // Handle clicks on the accessibility status card
    fun toggleAccessibilityService() {
        viewModelScope.launch(Dispatchers.IO) {
            val currentState = _uiState.value

            // Attempt to enable automatically via Shizuku if authorized
            if (!currentState.isAccessibilityEnabled && currentState.isShizukuAuthorized) {
                try {
                    privilegedManager.setAccessibilityServiceState(true)
                    _effect.send(HomeSideEffect.ShowToast("Attempting to start service via Shizuku..."))

                    // Poll briefly to await the background service startup
                    repeat(6) {
                        delay(500)
                        refreshState()
                        if (_uiState.value.isAccessibilityEnabled) {
                            return@launch
                        }
                    }

                    // Fallback if polling times out
                    if (!_uiState.value.isAccessibilityEnabled) {
                        _effect.send(HomeSideEffect.ShowToast("Start timeout, please enable manually"))
                    }

                } catch (e: Exception) {
                    e.printStackTrace()
                    _effect.send(HomeSideEffect.ShowToast("Shizuku start failed, please enable manually"))
                    _effect.send(HomeSideEffect.OpenAccessibilitySettings)
                }
            } else {
                // Navigate to system settings if Shizuku is unavailable
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

    fun refreshState() {
        viewModelScope.launch(Dispatchers.IO) {
            val accessibility = ThemeInstallAccessibilityService.isAccessibilityServiceEnabled(
                context,
                ThemeInstallAccessibilityService::class.java
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

            // 核心修复：直接向系统查验该服务是否在运行队列中
            val keepAliveRunning = KeepAliveService.isRunning(context)

            // Update state flow to trigger UI recomposition
            _uiState.update { state ->
                state.copy(
                    isAccessibilityEnabled = accessibility,
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
            // Unregister the updated receiver
            context.unregisterReceiver(serviceLifecycleReceiver)
        } catch (e: Exception) {
            // Ignore
        }
    }
}