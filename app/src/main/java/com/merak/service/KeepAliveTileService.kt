package com.merak.service

import android.service.quicksettings.Tile
import android.service.quicksettings.TileService
import com.google.android.accessibility.selecttospeak.SelectToSpeakService
import com.merak.core.os.shizuku.PrivilegedManager
import com.merak.data.settings.repo.SettingsRepo
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.koin.core.component.KoinComponent
import org.koin.core.component.inject
import timber.log.Timber

class KeepAliveTileService : TileService(), KoinComponent {

    private val settingsRepo: SettingsRepo by inject()
    private val privilegedManager: PrivilegedManager by inject()
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Main.immediate)

    override fun onStartListening() {
        super.onStartListening()
        autoStartKeepAlive()
        updateTile()
    }

    override fun onClick() {
        super.onClick()
        autoStartKeepAlive()
        updateTile()
    }

    override fun onStopListening() {
        updateTile()
        super.onStopListening()
    }

    override fun onDestroy() {
        scope.cancel()
        super.onDestroy()
    }

    private fun autoStartKeepAlive() {
        scope.launch {
            try {
                val settings = settingsRepo.appSettings.first()
                if (!settings.isKeepAliveEnabled) return@launch

                val isA11yEnabled = ThemeInstallAccessibilityService.isAccessibilityServiceEnabled(
                    applicationContext,
                    SelectToSpeakService::class.java
                )
                if (!isA11yEnabled) {
                    withContext(Dispatchers.IO) {
                        privilegedManager.setAccessibilityServiceState(true)
                    }
                }

                if (!KeepAliveService.isServiceRunning(applicationContext)) {
                    KeepAliveService.start(applicationContext)
                }
            } catch (e: Throwable) {
                Timber.tag(TAG).e(e, "Failed to auto-start keep-alive from tile")
            }
        }
    }

    private fun updateTile() {
        val tile = qsTile ?: return
        tile.state = if (KeepAliveService.isServiceRunning(this)) {
            Tile.STATE_ACTIVE
        } else {
            Tile.STATE_INACTIVE
        }
        tile.updateTile()
    }

    private companion object {
        const val TAG = "KeepAliveTileService"
    }
}
