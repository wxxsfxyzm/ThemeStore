package com.merak.di

import androidx.datastore.preferences.core.PreferenceDataStoreFactory
import androidx.datastore.preferences.preferencesDataStoreFile
import com.merak.data.settings.local.AppDataStore
import com.merak.data.settings.repo.SettingsRepo
import com.merak.data.settings.repo.SettingsRepoImpl
import org.koin.android.ext.koin.androidContext
import org.koin.core.module.dsl.bind
import org.koin.core.module.dsl.singleOf
import org.koin.core.qualifier.named
import org.koin.dsl.module

val settingsModule = module {
    single {
        PreferenceDataStoreFactory.create {
            androidContext().preferencesDataStoreFile("themestore_prefs")
        }
    }

    singleOf(::AppDataStore)

    single<SettingsRepo> {
        SettingsRepoImpl(
            dataStore = get(),
            appScope = get(named("AppScope"))
        )
    }
}
