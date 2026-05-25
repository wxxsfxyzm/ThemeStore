package com.merak.core.installer.impl

import android.annotation.SuppressLint
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.ParcelFileDescriptor
import com.merak.core.installer.IThemeInstaller
import com.merak.core.os.shizuku.PrivilegedManager
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.koin.core.component.KoinComponent
import org.koin.core.component.inject
import timber.log.Timber
import java.io.File
import java.io.InputStream
import java.net.URL

object ShizukuThemeInstaller : IThemeInstaller, KoinComponent {

    // Inject PrivilegedManager to communicate with Shizuku
    private val privilegedManager: PrivilegedManager by inject()

    @SuppressLint("SdCardPath")
    private const val TARGET_THEME_PATH = "/sdcard/Android/data/com.android.thememanager/files/theme/安装主题.mtz"

    /**
     * Core function to pipe any InputStream into the restricted target path via IPC FileDescriptor
     */
    private suspend fun streamToTarget(streamProvider: () -> InputStream?) =
        withContext(Dispatchers.IO) {
            try {
                // Get the file descriptor of the target file via Shizuku
                val pfd = privilegedManager.openRestrictedTargetFile(TARGET_THEME_PATH)
                    ?: return@withContext Result.failure(Exception("Failed to obtain restricted FD via Shizuku"))

                // Open the source stream
                val input = streamProvider()
                    ?: return@withContext Result.failure(Exception("Failed to open source stream"))

                // Pipe the stream directly to the target path via IPC
                input.use { inputStream ->
                    ParcelFileDescriptor.AutoCloseOutputStream(pfd).use { outputStream ->
                        inputStream.copyTo(outputStream)
                        outputStream.flush() // Ensure data is fully flushed to disk
                    }
                }

                Result.success(File(TARGET_THEME_PATH))

            } catch (e: Exception) {
                Timber.e(e, "Shizuku streaming failed")
                Result.failure(e)
            }
        }

    override suspend fun installThemeFromPath(context: Context, sourcePath: String): Result<File> {
        val sourceFile = File(sourcePath)
        if (!sourceFile.exists() || !sourceFile.canRead()) {
            return Result.failure(Exception("Source file cannot be read"))
        }
        return streamToTarget { sourceFile.inputStream() }
    }

    override suspend fun installThemeFromUri(context: Context, sourceUri: Uri) =
        streamToTarget { context.contentResolver.openInputStream(sourceUri) }

    override suspend fun installThemeFromUrl(context: Context, url: String) =
        streamToTarget { URL(url).openStream() }

    override fun applyTheme(context: Context, flags: Long) =
        try {
            Timber.d("Applying theme via Binder Hook with flags: 0x${flags.toString(16).uppercase()}")

            val intent = Intent(Intent.ACTION_MAIN).apply {
                setClassName(
                    "com.android.thememanager",
                    "com.android.thememanager.ApplyThemeForScreenshot"
                )
                putExtra("theme_file_path", TARGET_THEME_PATH)
                putExtra("api_called_from", "ThemeEditor")
                putExtra("ver2_step", "ver2_step_apply")
                putExtra("resource_type", "theme")
                putExtra("theme_apply_flags", flags)
                putExtra("theme_remove_flags", 0L)
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }

            privilegedManager.startActivityPrivileged(intent)
        } catch (e: Exception) {
            Timber.e(e, "Failed to apply theme via Shizuku Hidden API")
            false
        }
}
