package com.merak.core.installer.impl

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Environment
import com.merak.core.installer.IThemeInstaller
import com.merak.service.ThemeInstallAccessibilityService
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import timber.log.Timber
import java.io.File
import java.net.URL

/**
 * Implementation of theme installation using the zero-width space vulnerability.
 * Requires no special privileges.
 */
object NoneThemeInstaller : IThemeInstaller {

    private const val MTZ_FILE_NAME = "安装主题.mtz" // Keep filename as is to ensure compatibility

    private fun getReviseFile(file: File): File {
        val androidPath = Environment.getExternalStorageDirectory()?.path + "/Android/"
        val canonicalPath = try {
            file.canonicalPath
        } catch (e: Exception) {
            file.absolutePath
        }

        if (canonicalPath.length > androidPath.length &&
            canonicalPath.startsWith(androidPath, ignoreCase = true)
        ) {
            val revisedPath = androidPath + "\u200b" + canonicalPath.substring(androidPath.length)
            return File(revisedPath)
        }

        return file
    }

    private fun getThemeDirectory(): File {
        val themeManagerDir = File(
            Environment.getExternalStorageDirectory(),
            "Android/data/com.android.thememanager/files/theme"
        )

        val revisedDir = getReviseFile(themeManagerDir)
        if (!revisedDir.exists()) revisedDir.mkdirs()

        return revisedDir
    }

    private fun getThemeInstallFile(): File {
        val themeDir = getThemeDirectory()
        return File(themeDir, MTZ_FILE_NAME)
    }

    override suspend fun installThemeFromPath(context: Context, sourcePath: String): Result<File> {
        return withContext(Dispatchers.IO) {
            try {
                val sourceFile = File(sourcePath)

                if (!sourceFile.exists()) {
                    return@withContext Result.failure(Exception("Source file does not exist"))
                }

                if (!sourceFile.canRead()) {
                    return@withContext Result.failure(Exception("Source file cannot be read"))
                }

                val targetFile = getThemeInstallFile()
                if (targetFile.exists()) targetFile.delete()

                sourceFile.inputStream().use { input ->
                    targetFile.outputStream().use { output ->
                        val buffer = ByteArray(8192)
                        var bytes = input.read(buffer)
                        while (bytes >= 0) {
                            output.write(buffer, 0, bytes)
                            bytes = input.read(buffer)
                        }
                    }
                }

                if (!targetFile.exists() || targetFile.length() == 0L) {
                    return@withContext Result.failure(Exception("File copy failed"))
                }

                Result.success(targetFile)
            } catch (e: Exception) {
                Result.failure(e)
            }
        }
    }

    override suspend fun installThemeFromUri(context: Context, sourceUri: Uri): Result<File> {
        return withContext(Dispatchers.IO) {
            try {
                val targetFile = getThemeInstallFile()
                if (targetFile.exists()) targetFile.delete()

                context.contentResolver.openInputStream(sourceUri)?.use { input ->
                    targetFile.outputStream().use { output ->
                        input.copyTo(output)
                    }
                }

                if (!targetFile.exists() || targetFile.length() == 0L) {
                    return@withContext Result.failure(Exception("File copy failed"))
                }

                Result.success(targetFile)
            } catch (e: Exception) {
                Result.failure(e)
            }
        }
    }

    override suspend fun installThemeFromUrl(context: Context, url: String): Result<File> {
        return withContext(Dispatchers.IO) {
            try {
                val targetFile = getThemeInstallFile()
                if (targetFile.exists()) targetFile.delete()

                URL(url).openStream().use { input ->
                    targetFile.outputStream().use { output ->
                        input.copyTo(output)
                    }
                }

                if (!targetFile.exists() || targetFile.length() == 0L) {
                    return@withContext Result.failure(Exception("File download failed"))
                }

                Result.success(targetFile)
            } catch (e: Exception) {
                Result.failure(e)
            }
        }
    }

    override fun applyTheme(context: Context, flags: Long): Boolean {
        return try {
            val themeFile = getThemeInstallFile()
            if (!themeFile.exists()) return false

            val originalPath = "/sdcard/Android/data/com.android.thememanager/files/theme/安装主题.mtz"
            Timber.d("Applying theme with flags: 0x${flags.toString(16).uppercase()}")

            val intent = Intent().apply {
                action = Intent.ACTION_MAIN
                setClassName(
                    "com.android.thememanager",
                    "com.android.thememanager.ApplyThemeForScreenshot"
                )
                putExtra("theme_file_path", originalPath)
                putExtra("api_called_from", "ThemeEditor")
                putExtra("ver2_step", "ver2_step_apply")
                putExtra("resource_type", "theme")
                putExtra("theme_apply_flags", flags)
                putExtra("theme_remove_flags", 0L)
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }

            context.startActivity(intent)

            // Notify the watchdog to refresh the notification
            ThemeInstallAccessibilityService.requestRefresh()

            true
        } catch (e: Exception) {
            Timber.tag("ThemeInstaller").e(e, "Launch failed")
            false
        }
    }
}