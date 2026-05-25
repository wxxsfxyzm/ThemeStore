package com.merak.core.installer

import android.content.Context
import android.content.pm.PackageManager
import android.net.Uri
import com.merak.core.installer.impl.NoneThemeInstaller
import com.merak.core.installer.impl.ShizukuThemeInstaller
import rikka.shizuku.Shizuku

class ThemeInstallerManager(private val context: Context) {

    enum class Mode {
        NONE,
        SHIZUKU
    }

    // Use a custom getter to determine the mode dynamically at runtime
    // instead of saving it locally.
    val currentMode: Mode
        get() = try {
            // Check if Shizuku is alive and permission is granted
            if (Shizuku.pingBinder() && Shizuku.checkSelfPermission() == PackageManager.PERMISSION_GRANTED) {
                Mode.SHIZUKU
            } else {
                Mode.NONE
            }
        } catch (e: Exception) {
            // Fallback to NONE if Shizuku throws an exception (e.g., binder dead)
            Mode.NONE
        }

    private fun getActiveInstaller(): IThemeInstaller =
        when (currentMode) {
            Mode.NONE -> NoneThemeInstaller
            Mode.SHIZUKU -> ShizukuThemeInstaller
        }

    suspend fun installThemeFromPath(sourcePath: String) = getActiveInstaller().installThemeFromPath(context, sourcePath)

    suspend fun installThemeFromUri(sourceUri: Uri) = getActiveInstaller().installThemeFromUri(context, sourceUri)

    suspend fun installThemeFromUrl(url: String) = getActiveInstaller().installThemeFromUrl(context, url)

    fun applyTheme(flags: Long = ThemeFlags.ALL) = getActiveInstaller().applyTheme(context, flags)

}