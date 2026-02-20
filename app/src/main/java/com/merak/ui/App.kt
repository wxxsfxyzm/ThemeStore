package com.merak.ui

import android.widget.Toast
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.tween
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.merak.ui.activity.MainUiState
import com.merak.ui.activity.MainViewModel
import com.merak.ui.page.AboutPage
import com.merak.ui.page.FilePickerPage
import com.merak.ui.page.InstallEvent
import com.merak.ui.page.MainPage
import com.merak.ui.page.ThemeInstallPage
import com.merak.ui.page.ThemeInstallViewModel
import com.merak.ui.page.home.log.LogPage
import com.merak.ui.page.settings.theme.AppearancePage
import com.merak.ui.page.welcome.WelcomePage
import dev.chrisbanes.haze.HazeState
import org.koin.androidx.compose.koinViewModel

@Composable
fun App(
    uiState: MainUiState,
    mainViewModel: MainViewModel
) {
    // 1. 如果数据还没加载完（DataStore读取中），可以显示空白或Loading，避免闪烁
    if (!uiState.isLoaded) return

    // 2. 状态提升：根据 DataStore 中的状态决定初始页面
    val startDestination = remember {
        if (uiState.showWelcome) Route.WELCOME else Route.MAIN
    }

    val navController = rememberNavController()
    val hazeState = if (uiState.useBlur) remember { HazeState() } else null

    NavHost(
        navController = navController,
        modifier = Modifier.fillMaxSize(),
        startDestination = startDestination,
        // 统一动画配置，避免重复代码
        enterTransition = {
            slideInHorizontally(
                initialOffsetX = { it },
                animationSpec = tween(500, easing = FastOutSlowInEasing)
            )
        },
        exitTransition = {
            slideOutHorizontally(
                targetOffsetX = { -it / 5 },
                animationSpec = tween(500, easing = FastOutSlowInEasing)
            )
        },
        popEnterTransition = {
            slideInHorizontally(
                initialOffsetX = { -it / 5 },
                animationSpec = tween(500, easing = FastOutSlowInEasing)
            )
        },
        popExitTransition = {
            slideOutHorizontally(
                targetOffsetX = { it },
                animationSpec = tween(500, easing = FastOutSlowInEasing)
            )
        }
    ) {
        // --- 欢迎/引导页 ---
        composable(Route.WELCOME) {
            val pagerState = rememberPagerState(
                initialPage = uiState.initialWelcomePage,
                pageCount = { 5 }
            )

            // 监听页码变化并保存到 DataStore
            LaunchedEffect(pagerState) {
                snapshotFlow { pagerState.currentPage }.collect { page ->
                    mainViewModel.saveWelcomeProgress(page)
                }
            }

            WelcomePage(
                // 假设 WelcomePage 有回调，当点击"开始体验"时触发
                onFinish = {
                    mainViewModel.completeOnboarding() // 写 DataStore
                    // 导航到主页并移除回退栈
                    navController.navigate(Route.MAIN) {
                        popUpTo(Route.WELCOME) { inclusive = true }
                    }
                },
                pagerState = pagerState
            )
        }

        // --- 主页 ---
        composable(Route.MAIN) {
            MainPage(navController, hazeState)
        }

        // --- 主题安装页 ---
        composable(Route.THEME_INSTALL) {
            ThemeInstallPage(
                onBack = { navController.popBackStack() },
                onNavigateToFilePicker = { navController.navigate(Route.FILE_PICKER) }
            )
        }

        // --- 文件选择页 (重构重点) ---
        composable(Route.FILE_PICKER) {
            val context = LocalContext.current
            // 使用 Koin 注入专属的 ViewModel，将逻辑抽离出 UI
            val installViewModel: ThemeInstallViewModel = koinViewModel()

            // 监听 ViewModel 发出的事件 (Toast, 导航等)
            LaunchedEffect(Unit) {
                installViewModel.installEvent.collect { event ->
                    when (event) {
                        is InstallEvent.ShowToast -> {
                            Toast.makeText(context, event.message, Toast.LENGTH_SHORT).show()
                        }

                        is InstallEvent.NavigateBack -> {
                            navController.popBackStack()
                        }
                    }
                }
            }

            FilePickerPage(
                onBack = { navController.popBackStack() },
                onFileSelected = { file, flags ->
                    // 业务逻辑委托给 ViewModel
                    installViewModel.installLocalTheme(context, file, flags)
                }
            )
        }

        composable(Route.LOG) {
            LogPage(onBack = { navController.popBackStack() })
        }

        composable(Route.ABOUT) {
            AboutPage(onBack = { navController.popBackStack() })
        }

        composable(Route.APPEARANCE) {
            AppearancePage(navController)
        }
    }
}