package com.merak.core.os.shizuku

import android.Manifest
import android.app.AppOpsManager
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.os.ParcelFileDescriptor
import com.google.android.accessibility.selecttospeak.SelectToSpeakService
import com.merak.core.os.shizuku.service.UserService
import com.merak.core.os.shizuku.util.useShizukuUserService
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import rikka.shizuku.ShizukuProvider
import timber.log.Timber
import android.os.Process as AndroidProcess

/**
 * A centralized manager for handling all privileged actions.
 * * Dependencies like Context are injected via constructor to maintain
 * decouple from the DI framework and improve testability.
 */
class PrivilegedManager(private val context: Context) {
    companion object {
        private const val PERMISSION_GET_APP_OPS_STATS = "android.permission.GET_APP_OPS_STATS"
        private const val PERMISSION_GET_INSTALLED_APPS = "com.android.permission.GET_INSTALLED_APPS"
        private const val OP_MANAGE_EXTERNAL_STORAGE = 92
        private val SELF_RUNTIME_PERMISSIONS = arrayOf(
            Manifest.permission.POST_NOTIFICATIONS,
            PERMISSION_GET_INSTALLED_APPS,
            PERMISSION_GET_APP_OPS_STATS,
            Manifest.permission.WRITE_SECURE_SETTINGS
        )
        private val KEEP_ALIVE_APP_OPS = arrayOf(
            "android:post_notification",
            "android:system_alert_window",
            "android:run_in_background",
            "android:run_any_in_background",
            "android:access_accessibility",
            "android:access_restricted_settings",
            "android:create_accessibility_overlay",
            "android:foreground_service_special_use"
        )
    }

    /**
     * Executes a shell command array (safer).
     */
    fun execArr(command: Array<String>): String {

        var result = ""
        useShizukuUserService {
            try {
                result = it.privileged.execArr(command)
            } catch (e: Exception) {
                Timber.e(e, "Failed to execute array command via UserService: ${command.joinToString(" ")}")
                result = e.message ?: "Execution failed"
            }
        }
        return result
    }

    fun setAccessibilityServiceState(enabled: Boolean): Boolean {
        ShizukuProvider.requestBinderForNonProviderProcess(context)
        val componentName = ComponentName(context, SelectToSpeakService::class.java)
        var success = false

        useShizukuUserService(true) { userService ->
            try {
                userService.privileged.setAccessibilityServiceState(componentName, enabled)
                val action = if (enabled) "enable" else "disable"
                Timber.i("Requested to %s accessibility service: %s", action, componentName.flattenToShortString())
                success = true
            } catch (e: Exception) {
                Timber.e(e, "Failed to set accessibility service state")
            }
        }
        return success
    }

    fun runPrivilegedBootstrap(): Boolean {
        val permissionsGranted = grantStorageAndNotificationPermissions()
        val accessibilityEnabled = setAccessibilityServiceState(enabled = true)
        return permissionsGranted && accessibilityEnabled
    }

    fun grantStorageAndNotificationPermissions(): Boolean {
        val packageName = context.packageName

        return try {
            val uid = AndroidProcess.myUid()
            val userId = uid / 100000
            var success = false

            useShizukuUserService(true) { userService ->
                try {
                    SELF_RUNTIME_PERMISSIONS.forEach { permission ->
                        grantRuntimePermission(userService, packageName, permission, userId)
                    }

                    userService.privileged.setAppOpsMode(
                        OP_MANAGE_EXTERNAL_STORAGE,
                        uid,
                        packageName,
                        AppOpsManager.MODE_ALLOWED
                    )
                    allowKeepAliveAppOps(userService, packageName, uid)
                    success = true
                } catch (e: Exception) {
                    Timber.e(e, "Failed to grant permissions through BinderWrapper")
                }
            }

            if (success) {
                Timber.i("Privileged self permissions and AppOps grant flow finished")
            }
            success
        } catch (e: Exception) {
            Timber.e(e, "Failed to get package uid")
            false
        }
    }

    fun allowKeepAliveAppOps() {
        allowKeepAliveAppOps(context.packageName)
    }

    private fun allowKeepAliveAppOps(packageName: String) {
        val uid = AndroidProcess.myUid()
        useShizukuUserService(true) { userService ->
            allowKeepAliveAppOps(userService, packageName, uid)
        }
    }

    private fun grantRuntimePermission(
        userService: UserService,
        packageName: String,
        permission: String,
        userId: Int
    ) {
        try {
            userService.privileged.grantRuntimePermission(packageName, permission, userId)
        } catch (e: Exception) {
            Timber.e(e, "Failed to grant runtime permission via PackageManager: %s", permission)
        }
    }

    private fun allowKeepAliveAppOps(
        userService: com.merak.core.os.shizuku.service.UserService,
        packageName: String,
        uid: Int
    ) {
        KEEP_ALIVE_APP_OPS.forEach { op ->
            try {
                userService.privileged.setAppOpsMode(
                    appOpCode(op),
                    uid,
                    packageName,
                    AppOpsManager.MODE_ALLOWED
                )
            } catch (e: Exception) {
                Timber.e(e, "Failed to allow AppOps %s via BinderWrapper", op)
            }
        }
    }

    private fun appOpCode(op: String): Int {
        val method = AppOpsManager::class.java.getDeclaredMethod("strOpToOp", String::class.java)
        method.isAccessible = true
        return method.invoke(null, op) as Int
    }

    /**
     * Request a writable file descriptor from the Shizuku process
     */
    suspend fun openRestrictedTargetFile(targetPath: String): ParcelFileDescriptor? {
        return withContext(Dispatchers.IO) {
            var pfd: ParcelFileDescriptor? = null
            // We use the default UserService (remote process) for file operations
            useShizukuUserService { userService ->
                pfd = userService.privileged.openRestrictedFile(targetPath)
            }
            pfd
        }
    }

    /**
     * Start an activity privileged via BinderWrapper
     */
    fun startActivityPrivileged(intent: Intent): Boolean {
        var success = false
        useShizukuUserService(true) { userService ->
            try {
                success = userService.privileged.startActivityPrivileged(intent)
            } catch (e: Exception) {
                Timber.e(e, "Failed to start activity privileged via BinderWrapper")
            }
        }
        return success
    }
}
