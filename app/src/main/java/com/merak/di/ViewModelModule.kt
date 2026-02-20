package com.merak.di

import com.merak.ui.activity.MainViewModel
import com.merak.ui.page.ThemeInstallViewModel
import com.merak.ui.page.home.HomeViewModel
import com.merak.ui.page.home.log.LogViewModel
import com.merak.ui.page.settings.SettingsViewModel
import com.merak.ui.page.settings.theme.AppearanceViewModel
import com.merak.ui.page.welcome.WelcomeViewModel
import org.koin.android.ext.koin.androidApplication
import org.koin.core.module.dsl.viewModel
import org.koin.dsl.module

val viewModelModule = module {
    viewModel { WelcomeViewModel(androidApplication(), get()) }
    viewModel { MainViewModel(get()) }
    viewModel { HomeViewModel(androidApplication(), get(), get()) }
    viewModel { LogViewModel(androidApplication()) }
    viewModel { SettingsViewModel(androidApplication(), get()) }
    viewModel { AppearanceViewModel(get()) }
    viewModel { ThemeInstallViewModel(get()) }
}