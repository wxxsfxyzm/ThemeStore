package com.merak.ui

import android.widget.Toast
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.lifecycle.viewmodel.navigation3.rememberViewModelStoreNavEntryDecorator
import androidx.navigation3.runtime.NavEntryDecorator
import androidx.navigation3.runtime.NavKey
import androidx.navigation3.runtime.entryProvider
import androidx.navigation3.runtime.rememberDecoratedNavEntries
import androidx.navigation3.runtime.rememberNavBackStack
import androidx.navigation3.runtime.rememberSaveableStateHolderNavEntryDecorator
import androidx.navigation3.scene.SceneInfo
import androidx.navigation3.scene.SinglePaneSceneStrategy
import androidx.navigation3.scene.rememberSceneState
import androidx.navigation3.ui.NavDisplay
import androidx.navigation3.ui.NavDisplayTransitionEffects
import androidx.navigationevent.compose.NavigationBackHandler
import androidx.navigationevent.compose.NavigationEventState
import androidx.navigationevent.compose.rememberNavigationEventState
import com.merak.ui.activity.MainUiState
import com.merak.ui.activity.MainViewModel
import com.merak.ui.navigation.MiuixPredictiveBackAnimation
import com.merak.ui.page.AboutPage
import com.merak.ui.page.FilePickerPage
import com.merak.ui.page.InstallEvent
import com.merak.ui.page.MainPage
import com.merak.ui.page.ThemeInstallPage
import com.merak.ui.page.ThemeInstallViewModel
import com.merak.ui.page.home.log.LogPage
import com.merak.ui.page.settings.theme.AppearancePage
import com.merak.ui.page.welcome.WelcomePage
import kotlinx.coroutines.launch
import org.koin.androidx.compose.koinViewModel

@Composable
fun App(
    uiState: MainUiState,
    mainViewModel: MainViewModel
) {
    if (!uiState.isLoaded) return

    val startRoute = remember {
        if (uiState.showWelcome) Route.Welcome else Route.Main
    }
    val backStack = rememberNavBackStack(startRoute)
    val navigator = remember(backStack) { Navigator(backStack) }
    val predictiveBackAnimation = remember { MiuixPredictiveBackAnimation() }

    CompositionLocalProvider(LocalNavigator provides navigator) {
        var gestureState: NavigationEventState<SceneInfo<NavKey>>? = null
        val navigationScope = rememberCoroutineScope()
        val onBack: (() -> Unit) -> Unit = { callback ->
            navigationScope.launch {
                callback()
                navigator.pop()
            }
        }

        val entries = rememberDecoratedNavEntries(
            backStack = navigator.backStack,
            entryDecorators = listOf(
                rememberSaveableStateHolderNavEntryDecorator(),
                rememberViewModelStoreNavEntryDecorator(),
                NavEntryDecorator { content ->
                    Box(modifier = Modifier.fillMaxSize()) {
                        content.Content()
                    }
                }
            ),
            entryProvider = entryProvider {
                entry<Route.Welcome> {
                    val pagerState = rememberPagerState(
                        initialPage = uiState.initialWelcomePage,
                        pageCount = { 5 }
                    )

                    LaunchedEffect(pagerState) {
                        snapshotFlow { pagerState.currentPage }.collect { page ->
                            mainViewModel.saveWelcomeProgress(page)
                        }
                    }

                    WelcomePage(
                        onFinish = {
                            mainViewModel.completeOnboarding()
                            navigator.replaceAll(listOf(Route.Main))
                        },
                        pagerState = pagerState
                    )
                }

                entry<Route.Main> {
                    MainPage(
                        useBlur = uiState.useBlur,
                        useFloatingBottomBar = uiState.useAppleFloatingBar
                    )
                }

                entry<Route.ThemeInstall> {
                    ThemeInstallPage(
                        onBack = { navigator.pop() },
                        onNavigateToFilePicker = { navigator.push(Route.FilePicker) }
                    )
                }

                entry<Route.FilePicker> {
                    val context = LocalContext.current
                    val installViewModel: ThemeInstallViewModel = koinViewModel()

                    LaunchedEffect(Unit) {
                        installViewModel.installEvent.collect { event ->
                            when (event) {
                                is InstallEvent.ShowToast -> {
                                    Toast.makeText(context, event.message, Toast.LENGTH_SHORT).show()
                                }

                                is InstallEvent.NavigateBack -> {
                                    navigator.pop()
                                }
                            }
                        }
                    }

                    FilePickerPage(
                        onBack = { navigator.pop() },
                        onFileSelected = { file, flags ->
                            installViewModel.installLocalTheme(context, file, flags)
                        }
                    )
                }

                entry<Route.Log> {
                    LogPage(onBack = { navigator.pop() })
                }

                entry<Route.About> {
                    AboutPage(
                        onBack = { navigator.pop() },
                        useBlur = uiState.useBlur
                    )
                }

                entry<Route.Appearance> {
                    AppearancePage(
                        onBack = { navigator.pop() },
                        enableBlur = uiState.useBlur
                    )
                }
            }
        )

        val sceneState = rememberSceneState(
            entries = entries,
            sceneStrategies = listOf(SinglePaneSceneStrategy()),
            sceneDecoratorStrategies = emptyList(),
            sharedTransitionScope = null,
            onBack = { onBack {} },
        )
        val scene = sceneState.currentScene
        val currentInfo = SceneInfo(scene)
        val previousSceneInfos = sceneState.previousScenes.map { SceneInfo(it) }
        gestureState = rememberNavigationEventState(
            currentInfo = currentInfo,
            backInfo = previousSceneInfos
        )

        NavigationBackHandler(
            state = gestureState,
            isBackEnabled = scene.previousEntries.isNotEmpty(),
            onBackCompleted = { callback -> onBack(callback) },
            onBackCancelled = { callback -> callback() }
        )

        NavDisplay(
            sceneState = sceneState,
            navigationEventState = gestureState,
            contentAlignment = Alignment.TopStart,
            sizeTransform = null,
            transitionEffects = NavDisplayTransitionEffects(
                blockInputDuringTransition = true
            ),
            predictivePopTransitionSpec = { swipeEdge ->
                with(predictiveBackAnimation) {
                    onPredictivePopTransitionSpec(swipeEdge = swipeEdge)
                }
            },
            popTransitionSpec = {
                with(predictiveBackAnimation) {
                    onPopTransitionSpec()
                }
            },
            transitionSpec = {
                with(predictiveBackAnimation) {
                    onTransitionSpec()
                }
            }
        )
    }
}
