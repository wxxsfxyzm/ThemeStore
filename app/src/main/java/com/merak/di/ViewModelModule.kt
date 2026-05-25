package com.merak.di

import com.merak.ui.activity.MainViewModel
import com.merak.ui.page.ThemeInstallViewModel
import com.merak.ui.page.home.HomeViewModel
import com.merak.ui.page.home.log.LogViewModel
import com.merak.ui.page.settings.SettingsViewModel
import com.merak.ui.page.settings.theme.AppearanceViewModel
import com.merak.ui.page.welcome.WelcomeViewModel
import org.koin.core.module.dsl.viewModelOf
import org.koin.dsl.module

val viewModelModule = module {
    viewModelOf(::WelcomeViewModel)
    viewModelOf(::MainViewModel)
    viewModelOf(::HomeViewModel)
    viewModelOf(::LogViewModel)
    viewModelOf(::SettingsViewModel)
    viewModelOf(::AppearanceViewModel)
    viewModelOf(::ThemeInstallViewModel)
}
