package com.merak.ui.page.welcome

import android.Manifest
import android.app.Application
import android.content.Context
import android.content.pm.PackageManager
import android.os.Environment
import android.provider.Settings
import androidx.core.content.ContextCompat
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.merak.core.os.shizuku.PrivilegedManager
import com.merak.data.settings.repo.SettingsRepo
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import rikka.shizuku.Shizuku
import timber.log.Timber

class WelcomeViewModel(
    private val context: Application,
    private val privilegedManager: PrivilegedManager,
    private val settingsRepo: SettingsRepo
) : ViewModel() {

    private val _isShizukuAvailable = MutableStateFlow(false)
    val isShizukuAvailable = _isShizukuAvailable.asStateFlow()

    private val _isShizukuGranted = MutableStateFlow(false)
    val isShizukuGranted = _isShizukuGranted.asStateFlow()

    private val _storageGranted = MutableStateFlow(false)
    val storageGranted = _storageGranted.asStateFlow()

    private val _accessibilityGranted = MutableStateFlow(false)
    val accessibilityGranted = _accessibilityGranted.asStateFlow()

    private val _notificationGranted = MutableStateFlow(false)
    val notificationGranted = _notificationGranted.asStateFlow()

    // The Shizuku callback only updates the state, it will NOT trigger any navigation
    private val shizukuListener = Shizuku.OnRequestPermissionResultListener { requestCode, grantResult ->
        if (requestCode == 1002 && grantResult == PackageManager.PERMISSION_GRANTED) {
            _isShizukuGranted.value = true
        }
    }

    init {
        try {
            Shizuku.addRequestPermissionResultListener(shizukuListener)
        } catch (e: Exception) {
            // Ignore
        }
    }

    override fun onCleared() {
        super.onCleared()
        try {
            Shizuku.removeRequestPermissionResultListener(shizukuListener)
        } catch (e: Exception) {
            // Ignore
        }
    }

    fun checkShizukuStatus() {
        viewModelScope.launch(Dispatchers.IO) {
            try {
                val available = Shizuku.pingBinder()
                _isShizukuAvailable.value = available
                if (available) {
                    _isShizukuGranted.value = Shizuku.checkSelfPermission() == PackageManager.PERMISSION_GRANTED
                } else {
                    _isShizukuGranted.value = false
                }
            } catch (e: Exception) {
                _isShizukuAvailable.value = false
                _isShizukuGranted.value = false
            }
        }
    }

    fun requestShizukuPermission() {
        if (_isShizukuAvailable.value && !_isShizukuGranted.value) {
            try {
                Shizuku.requestPermission(1002)
            } catch (e: Exception) {
                Timber.e(e, "Failed to request Shizuku permission")
            }
        }
    }

    // Refactored to a suspend function for cleaner coroutine control in the UI layer
    suspend fun grantPermissionsViaShizuku() {
        withContext(Dispatchers.IO) {
            if (privilegedManager.runPrivilegedBootstrap()) {
                settingsRepo.setShizukuPrivilegedBootstrapCompleted(true)
            }
        }
    }

    fun checkStandardPermissions(context: Context) {
        _storageGranted.value = Environment.isExternalStorageManager()

        val expectedService =
            "${context.packageName}/com.google.android.accessibility.selecttospeak.SelectToSpeakService"
        val enabledServices = Settings.Secure.getString(
            context.contentResolver,
            Settings.Secure.ENABLED_ACCESSIBILITY_SERVICES
        )
        _accessibilityGranted.value = enabledServices?.contains(expectedService) == true

        _notificationGranted.value = ContextCompat.checkSelfPermission(
            context,
            Manifest.permission.POST_NOTIFICATIONS
        ) == PackageManager.PERMISSION_GRANTED
    }
}
