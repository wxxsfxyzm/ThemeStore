package com.merak.core.os.reflect

import android.os.IBinder
import android.provider.Settings
import com.merak.core.os.shizuku.util.SettingsReflectionInfo

/**
 * Extensions for ReflectRepo
 * Pattern: (Target: Any/Class) -> (Name: String) -> (Optional: Class/Types) -> (Args)
 */

inline fun <reified T> ReflectManager.getStaticValue(name: String, clazz: Class<*>): T? =
    getStaticFieldValue(name, clazz) as? T

inline fun <reified T> ReflectManager.getValue(obj: Any, name: String, clazz: Class<*>? = null): T? =
    getFieldValue(obj, name, clazz ?: obj.javaClass) as? T

inline fun <reified T> ReflectManager.invoke(
    obj: Any,
    name: String,
    clazz: Class<*>? = null,
    parameterTypes: Array<Class<*>> = emptyArray(),
    vararg args: Any?
): T? = invokeMethod(obj, name, clazz ?: obj.javaClass, parameterTypes, *args) as? T

inline fun <reified T> ReflectManager.invokeStatic(
    name: String,
    clazz: Class<*>,
    parameterTypes: Array<Class<*>> = emptyArray(),
    vararg args: Any?
): T? = invokeStaticMethod(name, clazz, parameterTypes, *args) as? T

fun ReflectManager.resolveSettingsSecureBinder(): SettingsReflectionInfo? {
    val holder = this.getStaticValue<Any>("sProviderHolder", Settings.Secure::class.java) ?: return null
    val provider = this.getValue<Any>(holder, "mContentProvider") ?: return null

    val remoteField = this.getDeclaredField("mRemote", provider.javaClass) ?: return null
    val originalBinder = remoteField.get(provider) as? IBinder ?: return null

    return SettingsReflectionInfo(provider, remoteField, originalBinder)
}
