package com.merak.core.os.shizuku.util

// Update imports to the new ReflectManager location
import android.annotation.SuppressLint
import android.content.AttributionSource
import android.content.Context
import android.content.ContextWrapper
import android.content.pm.PackageManager
import android.os.IBinder
import android.os.Process
import com.merak.core.os.reflect.ReflectManager
import com.merak.core.os.reflect.resolveSettingsSecureBinder
import com.merak.core.os.shizuku.exception.ShizukuNotWorkException
import com.merak.x.BuildConfig
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.first
import rikka.shizuku.Shizuku
import rikka.shizuku.ShizukuBinderWrapper
import rikka.sui.Sui
import timber.log.Timber
import java.lang.reflect.Field

suspend fun <T> requireShizukuPermissionGranted(action: suspend () -> T): T {
    callbackFlow {
        Sui.init(BuildConfig.APPLICATION_ID)
        if (Shizuku.checkSelfPermission() == PackageManager.PERMISSION_GRANTED) {
            send(Unit)
            awaitClose()
        } else {
            val requestCode = (Int.MIN_VALUE..Int.MAX_VALUE).random()
            val listener =
                Shizuku.OnRequestPermissionResultListener { _requestCode, grantResult ->
                    if (_requestCode != requestCode) return@OnRequestPermissionResultListener
                    if (Shizuku.checkSelfPermission() == PackageManager.PERMISSION_GRANTED)
                        trySend(Unit)
                    else close(Exception("sui/shizuku permission denied"))
                }
            Shizuku.addRequestPermissionResultListener(listener)
            Shizuku.requestPermission(requestCode)
            awaitClose { Shizuku.removeRequestPermissionResultListener(listener) }
        }
    }.catch {
        throw ShizukuNotWorkException(it)
    }.first()

    return action()
}

class ShizukuContext(base: Context) : ContextWrapper(base) {
    override fun getOpPackageName(): String {
        return "com.android.shell"
    }

    override fun getAttributionSource(): AttributionSource {
        val shellUid = Shizuku.getUid()
        val builder = AttributionSource.Builder(shellUid)
            .setPackageName("com.android.shell")

        builder.setPid(Process.INVALID_PID)

        return builder.build()
    }
}

@SuppressLint("PrivateApi", "DiscouragedPrivateApi")
class ShizukuHook(private val reflectManager: ReflectManager) {

    val hookedSettingsBinder: IBinder? by lazy {
        Timber.tag("ShizukuHook").d("Creating on-demand hooked Settings Binder...")
        try {
            val info = reflectManager.resolveSettingsSecureBinder() ?: return@lazy null

            ShizukuBinderWrapper(info.originalBinder).also {
                Timber.tag("ShizukuHook").i("On-demand hooked Settings Binder created.")
            }
        } catch (e: Exception) {
            Timber.tag("ShizukuHook").e(e, "Failed to create hooked Settings Binder")
            null
        }
    }
}

data class SettingsReflectionInfo(
    val provider: Any,
    val remoteField: Field,
    val originalBinder: IBinder
)