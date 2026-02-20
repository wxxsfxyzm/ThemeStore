package com.merak.core.os.shizuku

import android.app.Application
import android.content.Context
import android.content.pm.PackageManager
import com.merak.data.settings.repo.SettingsRepo
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import rikka.shizuku.Shizuku
import timber.log.Timber

/**
 * Manages the automatic enabling of the accessibility service via Shizuku.
 * Dependencies are injected via constructor by Koin.
 */
class AutoAccessibilityManager(
    private val context: Context,
    private val settingsRepo: SettingsRepo,
    private val privilegedManager: PrivilegedManager
) {
    private val scope = CoroutineScope(Dispatchers.IO + SupervisorJob())
    private var isInitialized = false

    fun init() {
        if (Application.getProcessName() != context.packageName) return
        if (isInitialized) return
        isInitialized = true

        // Listen for Shizuku binder connection (the "troublesome" but robust way)
        Shizuku.addBinderReceivedListener {
            checkAndEnableAccessibility()
        }

        // Check immediately if already connected
        if (Shizuku.pingBinder()) {
            checkAndEnableAccessibility()
        }
    }

    /**
     * Public method to manually trigger the check,
     * e.g., when Shizuku permission is granted.
     */
    fun runCheck() {
        checkAndEnableAccessibility()
    }

    private fun checkAndEnableAccessibility() {
        scope.launch {
            try {
                if (Shizuku.checkSelfPermission() != PackageManager.PERMISSION_GRANTED) return@launch

                val settings = settingsRepo.appSettings.first()
                if (!settings.isKeepAliveEnabled) return@launch

                Timber.d("AutoAccessibilityManager: Executing background enable via Shizuku")
                privilegedManager.setAccessibilityServiceState(true)
            } catch (e: Exception) {
                Timber.e(e, "AutoAccessibilityManager: Check failed")
            }
        }
    }
}