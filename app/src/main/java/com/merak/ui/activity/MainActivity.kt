package com.merak.ui.activity

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.core.splashscreen.SplashScreen.Companion.installSplashScreen
import com.merak.ui.App
import com.merak.ui.theme.ThemeStoreMiuixTheme
import org.koin.androidx.compose.koinViewModel

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        val splashScreen = installSplashScreen()
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        window.isNavigationBarContrastEnforced = false

        setContent {
            val mainViewModel: MainViewModel = koinViewModel()
            val uiState by mainViewModel.uiState.collectAsState()
            splashScreen.setKeepOnScreenCondition { !uiState.isLoaded }
            if (uiState.isLoaded) ThemeStoreMiuixTheme(
                themeMode = uiState.themeMode,
                useDynamicColor = uiState.useDynamicColor,
                useMiuixMonet = uiState.useMiuixMonet,
                seedColor = uiState.seedColor
            ) { App(uiState, mainViewModel) }
        }
    }
}