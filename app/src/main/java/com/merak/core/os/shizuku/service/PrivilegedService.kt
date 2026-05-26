package com.merak.core.os.shizuku.service

import android.annotation.SuppressLint
import android.content.AttributionSource
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.os.IBinder
import android.os.ParcelFileDescriptor
import android.os.RemoteException
import android.provider.Settings
import android.util.Log
import com.merak.core.os.reflect.ReflectManager
import com.merak.core.os.reflect.invoke
import com.merak.core.os.reflect.invokeStatic
import com.merak.core.os.shizuku.util.ShizukuContext
import com.merak.core.os.shizuku.util.ShizukuHook
import com.merak.x.IUserService
import org.koin.core.component.KoinComponent
import org.koin.core.component.inject
import timber.log.Timber
import java.io.File
import java.io.IOException

@SuppressLint("LogNotTimber")
class PrivilegedService() : IUserService.Stub(), KoinComponent {

    companion object {
        private const val TAG = "PrivilegedService"
        private const val SHELL_PATH = "/system/bin/sh"
        private const val CALL_METHOD_USER_KEY = "_user"
        private const val CALL_METHOD_OVERRIDEABLE_BY_RESTORE_KEY = "_overrideable_by_restore"
        private const val CALL_METHOD_PUT_SECURE = "PUT_secure"
    }

    private val context by inject<Context>()
    private val reflect by inject<ReflectManager>()
    private val shizukuHook by inject<ShizukuHook>()

    @Throws(RemoteException::class)
    override fun execArr(command: Array<String>): String {
        val shellCmd = arrayOf(SHELL_PATH, "-c", command.toShellCommand())

        return try {
            val process = Runtime.getRuntime().exec(shellCmd)
            readResult(process)
        } catch (e: IOException) {
            throw RemoteException(e.message)
        } catch (e: InterruptedException) {
            Thread.currentThread().interrupt()
            throw RemoteException(e.message)
        }
    }

    override fun destroy() {
        // Cleanup resources if needed
        Log.d(TAG, "PrivilegedService destroy called")
    }

    private fun Array<String>.toShellCommand(): String {
        return this.joinToString(" ") { arg ->
            // Add single quotes to protect arguments containing spaces or special characters
            if (arg.contains(" ") || arg.contains("'") || arg.contains("\"")) {
                "'" + arg.replace("'", "'\\''") + "'"
            } else {
                arg
            }
        }
    }

    @Throws(IOException::class, InterruptedException::class)
    private fun readResult(process: Process): String {
        // Read standard output and standard error streams respectively
        val output = process.inputStream.bufferedReader().use { it.readText() }
        val error = process.errorStream.bufferedReader().use { it.readText() }

        // Wait for the command execution to complete and retrieve the exit code
        val exitCode = process.waitFor()

        // Check the exit code. 0 typically indicates success, any non-zero value indicates failure.
        if (exitCode != 0) {
            throw IOException(error.ifBlank { "Exit code: $exitCode" })
        }

        // Return the standard output content upon success
        return output
    }

    override fun setAccessibilityServiceState(componentName: ComponentName, enabled: Boolean) {
        val serviceString = componentName.flattenToString()

        try {
            val currentServicesStr = Settings.Secure.getString(
                context.contentResolver,
                Settings.Secure.ENABLED_ACCESSIBILITY_SERVICES
            )
            val currentServicesList = if (currentServicesStr.isNullOrEmpty()) {
                emptyList<String>()
            } else {
                currentServicesStr.split(":")
            }

            val newServicesList = currentServicesList.toMutableList()
            var isChanged = false

            // 2. Add or remove the target service based on the requested state
            if (enabled) {
                if (!newServicesList.contains(serviceString)) {
                    newServicesList.add(serviceString)
                    isChanged = true
                }
            } else {
                if (newServicesList.contains(serviceString)) {
                    newServicesList.remove(serviceString)
                    isChanged = true
                }
            }

            if (isChanged) {
                val newServicesStr = newServicesList.joinToString(":")
                val accessibilityEnabled = enabled || newServicesStr.isNotEmpty()

                putSecureSettingWithHookedProvider(
                    Settings.Secure.ENABLED_ACCESSIBILITY_SERVICES,
                    newServicesStr
                )
                putSecureSettingWithHookedProvider(
                    Settings.Secure.ACCESSIBILITY_ENABLED,
                    if (accessibilityEnabled) "1" else "0"
                )

                val action = if (enabled) "enabled" else "disabled"
                Timber.i("Successfully %s accessibility service via BinderWrapper: %s", action, serviceString)
            } else {
                Timber.d("Accessibility service state is already correct.")
            }
        } catch (e: Exception) {
            Timber.e(e, "Failed to set accessibility service state via BinderWrapper.")
            throw e
        }
    }

    private fun putSecureSettingWithHookedProvider(name: String, value: String) {
        val provider = hookedContentProvider(
            shizukuHook.hookedSettingsBinder
                ?: throw IllegalStateException("Hooked Settings binder is unavailable")
        )
        val extras = Bundle().apply {
            putString(Settings.NameValueTable.VALUE, value)
            putInt(CALL_METHOD_USER_KEY, android.os.Process.myUid() / 100000)
            putBoolean(CALL_METHOD_OVERRIDEABLE_BY_RESTORE_KEY, true)
        }

        reflect.invoke<Bundle>(
            obj = provider,
            name = "call",
            clazz = provider.javaClass,
            parameterTypes = arrayOf(
                AttributionSource::class.java,
                String::class.java,
                String::class.java,
                String::class.java,
                Bundle::class.java
            ),
            ShizukuContext(context).attributionSource,
            Settings.Secure.CONTENT_URI.authority,
            CALL_METHOD_PUT_SECURE,
            name,
            extras
        )
    }

    private fun hookedContentProvider(binder: IBinder): Any {
        val contentProviderNative = Class.forName("android.content.ContentProviderNative")
        return reflect.invokeStatic<Any>(
            name = "asInterface",
            clazz = contentProviderNative,
            parameterTypes = arrayOf(IBinder::class.java),
            binder
        ) ?: throw IllegalStateException("Failed to create hooked Settings provider")
    }

    @SuppressLint("PrivateApi")
    override fun grantRuntimePermission(packageName: String, permission: String, userId: Int) {
        try {
            // Retrieve the hooked PackageManager interface from ShizukuHook
            val iPackageManager = shizukuHook.hookedPackageManager

            // Invoke the grantRuntimePermission method using reflection
            reflect.invoke<Unit>(
                obj = iPackageManager,
                name = "grantRuntimePermission",
                clazz = iPackageManager.javaClass,
                parameterTypes = arrayOf(String::class.java, String::class.java, Int::class.javaPrimitiveType!!),
                packageName, permission, userId
            )
            Timber.i("Granted %s to %s via Hooked PM", permission, packageName)
        } catch (e: Exception) {
            Timber.e(e, "Failed to grant %s", permission)
        }
    }

    @SuppressLint("PrivateApi")
    override fun setAppOpsMode(code: Int, uid: Int, packageName: String, mode: Int) {
        try {
            // Retrieve the hooked AppOpsService interface from ShizukuHook
            val iAppOpsService = shizukuHook.hookedAppOpsService

            // Invoke the setMode method using reflection
            reflect.invoke<Unit>(
                obj = iAppOpsService,
                name = "setMode",
                clazz = iAppOpsService.javaClass,
                parameterTypes = arrayOf(
                    Int::class.javaPrimitiveType!!,
                    Int::class.javaPrimitiveType!!,
                    String::class.java,
                    Int::class.javaPrimitiveType!!
                ),
                code, uid, packageName, mode
            )
            Timber.i("Set AppOps mode %d to %d for %s via Hooked AppOps", code, mode, packageName)
        } catch (e: Exception) {
            Timber.e(e, "Failed to set AppOps mode")
        }
    }

    override fun openRestrictedFile(targetPath: String): ParcelFileDescriptor {
        val file = File(targetPath)
        val parent = file.parentFile

        // Create the directory structure if it does not exist using privileged access
        if (parent != null && !parent.exists()) {
            parent.mkdirs()
        }

        // Clear the old file if it already exists
        if (file.exists()) {
            file.delete()
        }

        file.createNewFile()

        // Return a write-only file descriptor across the IPC boundary
        return ParcelFileDescriptor.open(
            file,
            ParcelFileDescriptor.MODE_WRITE_ONLY or
                    ParcelFileDescriptor.MODE_CREATE or
                    ParcelFileDescriptor.MODE_TRUNCATE
        )
    }

    override fun startActivityPrivileged(intent: Intent): Boolean {
        try {
            // Retrieve the hooked ActivityManager interface
            val am = shizukuHook.hookedActivityManager

            // Use the Shell UID (2000) to initiate the request
            val userId = android.os.Process.myUid() / 100000
            val callerPackage = "com.android.shell"
            val resolvedType = intent.resolveType(context.contentResolver)

            // Invoke startActivityAsUser via the Hidden API
            val result = am.startActivityAsUser(
                null, // IApplicationThread caller
                callerPackage,
                intent,
                resolvedType,
                null, // resultTo
                null, // resultWho
                0,    // requestCode
                0,    // flags
                null, // profilerInfo
                null, // options
                userId
            )

            Timber.d("startActivityAsUser returned result code: \$result")
            // In the ActivityManager API, 0 (START_SUCCESS) or any positive number indicates success
            return result >= 0
        } catch (e: Exception) {
            Timber.e(e, "startActivityPrivileged failed with an exception")
            return false
        }
    }
}
