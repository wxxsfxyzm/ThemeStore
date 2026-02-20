package com.merak.ui.page.welcome

import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.PagerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import com.merak.ui.components.MiuixBackButton
import kotlinx.coroutines.launch
import org.koin.androidx.compose.koinViewModel
import top.yukonga.miuix.kmp.basic.Scaffold
import top.yukonga.miuix.kmp.basic.TopAppBar
import top.yukonga.miuix.kmp.theme.MiuixTheme

@Composable
fun WelcomePage(
    pagerState: PagerState,
    onFinish: () -> Unit,
    viewModel: WelcomeViewModel = koinViewModel()
) {
    val coroutineScope = rememberCoroutineScope()
    val isShizukuGranted by viewModel.isShizukuGranted.collectAsState()

    fun handleBackNavigation() {
        coroutineScope.launch {
            val current = pagerState.currentPage

            // Skip the LoadingPage (index 3) if returning from EnterPager (index 4) with Shizuku granted
            if (current == 4 && isShizukuGranted) {
                pagerState.animateScrollToPage(2)
            } else if (current > 0) {
                pagerState.animateScrollToPage(current - 1)
            }
        }
    }

    BackHandler(enabled = pagerState.currentPage > 0) {
        handleBackNavigation()
    }

    Scaffold(
        modifier = Modifier.fillMaxSize(),
        topBar = {
            TopAppBar(
                title = "",
                navigationIcon = {
                    AnimatedVisibility(
                        visible = pagerState.targetPage > 0,
                        enter = fadeIn(tween(200)) + slideInHorizontally(tween(200)) { it / 2 },
                        exit = fadeOut(tween(200)) + slideOutHorizontally(tween(200)) { it / 2 }
                    ) {
                        MiuixBackButton(
                            modifier = Modifier.padding(start = 16.dp),
                            onClick = { handleBackNavigation() }
                        )
                    }
                },
                color = Color.Transparent
            )
        },
        containerColor = MiuixTheme.colorScheme.background
    ) { paddingValues ->
        HorizontalPager(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .widthIn(max = 480.dp),
            state = pagerState,
            userScrollEnabled = false,
            pageContent = { page ->
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    when (page) {
                        0 -> WelcomeEnterPager(pagerState)
                        1 -> PrivacyPage(pagerState)
                        2 -> ShizukuPage(pagerState, viewModel)
                        // Swap to LoadingPage here instead of EnterPager
                        3 -> if (isShizukuGranted) LoadingPage(pagerState, viewModel)
                        else PermissionPage(pagerState, viewModel)

                        4 -> EnterPager(pagerState, 4, onFinish)
                    }
                }
            }
        )
    }
}