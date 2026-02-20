package com.merak.ui.page.welcome

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.pager.PagerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.filter
import kotlinx.coroutines.flow.first
import top.yukonga.miuix.kmp.basic.InfiniteProgressIndicator
import top.yukonga.miuix.kmp.basic.Text
import top.yukonga.miuix.kmp.theme.MiuixTheme

@Composable
fun LoadingPage(pagerState: PagerState, viewModel: WelcomeViewModel) {

    LaunchedEffect(Unit) {
        // 核心修复：挂起协程，只有当 Pager 真正将目标定为本页（Index 3）时，才放行向下执行。
        // 这完美阻止了 Pager 预加载导致的逻辑“抢跑”。
        snapshotFlow { pagerState.currentPage == 3 || pagerState.targetPage == 3 }
            .filter { it }
            .first()

        val startTime = System.currentTimeMillis()

        // 1. 挂起并等待底层通过 Shizuku 完成静默授权
        viewModel.grantPermissionsViaShizuku()

        // 2. 强制保证 Loading 页面至少显示 1200ms（1.2秒），防止 UI 闪烁
        val elapsedTime = System.currentTimeMillis() - startTime
        val minDuration = 2000L
        if (elapsedTime < minDuration) {
            delay(minDuration - elapsedTime)
        }

        // 3. 一切就绪后，安全跳转到最终页
        pagerState.animateScrollToPage(4)
    }

    Box(
        modifier = Modifier.fillMaxSize(),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            InfiniteProgressIndicator()
            Text(
                text = "正在处理授权...", // Can be replaced with stringResource
                style = MiuixTheme.textStyles.main
            )
        }
    }
}