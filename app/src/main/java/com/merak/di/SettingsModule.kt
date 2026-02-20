package com.merak.di

import androidx.datastore.preferences.core.PreferenceDataStoreFactory
import androidx.datastore.preferences.preferencesDataStoreFile
import com.merak.data.settings.local.AppDataStore
import com.merak.data.settings.repo.SettingsRepo
import com.merak.data.settings.repo.SettingsRepoImpl
import org.koin.android.ext.koin.androidContext
import org.koin.dsl.module

val settingsModule = module {
    single {
        PreferenceDataStoreFactory.create {
            androidContext().preferencesDataStoreFile("themestore_prefs")
        }
    }

    single { AppDataStore(get()) }

    single<SettingsRepo> { SettingsRepoImpl(get()) }
}