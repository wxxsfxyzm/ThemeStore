package com.merak.core.installer

import android.content.Context
import android.net.Uri
import java.io.File

/**
 * Defines the standard operations for theme installation.
 */
interface IThemeInstaller {

    suspend fun installThemeFromPath(context: Context, sourcePath: String): Result<File>

    suspend fun installThemeFromUri(context: Context, sourceUri: Uri): Result<File>

    suspend fun installThemeFromUrl(context: Context, url: String): Result<File>

    fun applyTheme(context: Context, flags: Long): Boolean

}
