package com.merak.core.installer

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Environment
import com.merak.service.KeepAliveService
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import timber.log.Timber
import java.io.File
import java.net.URL

class ThemeInstaller(private val context: Context) {
    companion object {
        const val MTZ_FILE_NAME = "安装主题.mtz"
    }

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

    fun getThemeInstallFile(): File {
        val themeDir = getThemeDirectory()
        return File(themeDir, MTZ_FILE_NAME)
    }

    suspend fun installThemeFromPath(sourcePath: String): Result<File> {
        return withContext(Dispatchers.IO) {
            try {
                val sourceFile = File(sourcePath)

                if (!sourceFile.exists()) {
                    return@withContext Result.failure(Exception("源文件不存在"))
                }

                if (!sourceFile.canRead()) {
                    return@withContext Result.failure(Exception("源文件无法读取"))
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
                    return@withContext Result.failure(Exception("文件复制失败"))
                }

                Result.success(targetFile)
            } catch (e: Exception) {
                Result.failure(e)
            }
        }
    }

    suspend fun installThemeFromUri(context: Context, sourceUri: Uri): Result<File> {
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
                    return@withContext Result.failure(Exception("文件复制失败"))
                }

                Result.success(targetFile)
            } catch (e: Exception) {
                Result.failure(e)
            }
        }
    }

    suspend fun installThemeFromUrl(url: String): Result<File> {
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
                    return@withContext Result.failure(Exception("文件下载失败"))
                }

                Result.success(targetFile)
            } catch (e: Exception) {
                Result.failure(e)
            }
        }
    }

    fun applyTheme(flags: Long = ThemeFlags.ALL): Boolean {
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

            KeepAliveService.requestRefresh(context)

            true
        } catch (e: Exception) {
            Timber.tag("ThemeInstaller").e(e, "启动失败")
            false
        }
    }

    suspend fun quickInstall(sourcePath: String, flags: Long = ThemeFlags.ALL): Result<Boolean> {
        return try {
            val result = installThemeFromPath(sourcePath)

            result.fold(
                onSuccess = {
                    // Call applyTheme without passing context, passing flags instead
                    val applied = applyTheme(flags)
                    if (applied) {
                        Result.success(true)
                    } else {
                        // Keep the original exception message logic
                        Result.failure(Exception("启动主题管理器失败"))
                    }
                },
                onFailure = { error ->
                    Result.failure(error)
                }
            )
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
}