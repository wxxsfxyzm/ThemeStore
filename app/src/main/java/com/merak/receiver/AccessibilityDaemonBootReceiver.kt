package com.merak.receiver

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import com.merak.data.settings.repo.SettingsRepo
import com.merak.service.AccessibilityDaemonService
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import org.koin.core.component.KoinComponent
import org.koin.core.component.inject
import timber.log.Timber

class AccessibilityDaemonBootReceiver : BroadcastReceiver(), KoinComponent {
    private val settingsRepo: SettingsRepo by inject()

    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action !in BOOT_ACTIONS) return

        val pendingResult = goAsync()
        CoroutineScope(SupervisorJob() + Dispatchers.IO).launch {
            try {
                val settings = settingsRepo.appSettings.first()
                if (
                    settings.isAccessibilityDaemonBootEnabled &&
                    settings.accessibilityDaemonServices.isNotEmpty()
                ) {
                    AccessibilityDaemonService.start(context)
                }
            } catch (e: Throwable) {
                Timber.tag(TAG).e(e, "Failed to start accessibility daemon after boot")
            } finally {
                pendingResult.finish()
            }
        }
    }

    private companion object {
        const val TAG = "AccessibilityDaemonBoot"
        val BOOT_ACTIONS = setOf(
            Intent.ACTION_BOOT_COMPLETED,
            Intent.ACTION_LOCKED_BOOT_COMPLETED,
            "android.intent.action.QUICKBOOT_POWERON"
        )
    }
}
