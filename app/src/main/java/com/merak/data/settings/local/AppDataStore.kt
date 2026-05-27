package com.merak.data.settings.local

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

class AppDataStore(
    private val dataStore: DataStore<Preferences>,
    // private val json: Json
) {
    val data: Flow<Preferences> = dataStore.data

    companion object {
        val ONBOARDING_COMPLETED = booleanPreferencesKey("onboarding_completed")
        val WELCOME_PAGE_INDEX = intPreferencesKey("welcome_page_index")
        val SHIZUKU_PRIVILEGED_BOOTSTRAP_COMPLETED =
            booleanPreferencesKey("shizuku_privileged_bootstrap_completed")

        val THEME_MODE = stringPreferencesKey("theme_mode")
        val THEME_USE_DYNAMIC_COLOR = booleanPreferencesKey("theme_use_dynamic_color")
        val THEME_PALETTE_STYLE = stringPreferencesKey("theme_palette_style")
        val THEME_COLOR_SPEC = stringPreferencesKey("theme_color_spec")
        val THEME_SEED_COLOR = intPreferencesKey("theme_seed_color")
        val UI_USE_MIUIX_MONET = booleanPreferencesKey("ui_use_miui_x_monet")
        val UI_USE_BLUR = booleanPreferencesKey("ui_use_blur")
        val UI_USE_APPLE_FLOATING_BAR = booleanPreferencesKey("ui_use_apple_floating_bar")

        val KEEP_ALIVE_ENABLED = booleanPreferencesKey("keep_alive_enabled")
        val OPTIMIZATION_MODE_ENABLED = booleanPreferencesKey("optimization_mode_enabled")
        val ACCESSIBILITY_PINNED_SERVICES = stringPreferencesKey("accessibility_pinned_services")
        val ACCESSIBILITY_DAEMON_SERVICES = stringPreferencesKey("accessibility_daemon_services")
        val ACCESSIBILITY_DAEMON_BOOT_ENABLED = booleanPreferencesKey("accessibility_daemon_boot_enabled")
        val ACCESSIBILITY_DAEMON_TOAST_ENABLED = booleanPreferencesKey("accessibility_daemon_toast_enabled")

    }

    suspend fun putString(key: Preferences.Key<String>, value: String) {
        dataStore.edit { it[key] = value }
    }

    fun getString(key: Preferences.Key<String>, default: String = ""): Flow<String> =
        dataStore.data.map { it[key] ?: default }

    suspend fun putInt(key: Preferences.Key<Int>, value: Int) {
        dataStore.edit { it[key] = value }
    }

    fun getInt(key: Preferences.Key<Int>, default: Int = 0): Flow<Int> =
        dataStore.data.map { it[key] ?: default }

    suspend fun putBoolean(key: Preferences.Key<Boolean>, value: Boolean) {
        dataStore.edit { it[key] = value }
    }

    fun getBoolean(key: Preferences.Key<Boolean>, default: Boolean = false): Flow<Boolean> =
        dataStore.data.map { it[key] ?: default }
}
