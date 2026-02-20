package com.merak.ui.page.welcome

import androidx.compose.animation.core.CubicBezierEasing
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.wrapContentHeight
import androidx.compose.foundation.pager.PagerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.merak.x.R
import top.yukonga.miuix.kmp.basic.Button
import top.yukonga.miuix.kmp.basic.Text
import top.yukonga.miuix.kmp.theme.MiuixTheme

@Composable
fun EnterPager(
    pagerState: PagerState,
    targetPageIndex: Int = 4,
    onFinish: () -> Unit
) {
    val easing = CubicBezierEasing(.42f, 0f, 0.26f, .85f)

    // Reintroduce the local state to trigger the animation
    val go = remember { mutableStateOf(false) }

    // Trigger animation when this page is the target or currently settled
    LaunchedEffect(pagerState.currentPage, pagerState.targetPage) {
        go.value = pagerState.currentPage == targetPageIndex || pagerState.targetPage == targetPageIndex
    }

    // Now the animation will run because it sees the state change from false to true
    val animatedY by animateDpAsState(
        targetValue = if (go.value) (-30).dp else 0.dp,
        animationSpec = tween(durationMillis = 1150, easing = easing),
        label = "offset"
    )
    val animatedAlpha by animateFloatAsState(
        targetValue = if (go.value) 1f else 0f, // Start from 0f for a proper fade-in
        animationSpec = tween(durationMillis = 1150, easing = easing),
        label = "alpha"
    )

    Column {
        Column(
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth()
                .padding(bottom = 100.dp)
                .offset(x = 0.dp, y = animatedY)
                .alpha(animatedAlpha),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Text(
                text = buildAnnotatedString {
                    withStyle(SpanStyle(color = MiuixTheme.colorScheme.onBackground)) {
                        append("Theme")
                    }
                    withStyle(SpanStyle(color = MiuixTheme.colorScheme.primary)) {
                        append("Store")
                    }
                },
                fontSize = 39.sp,
                fontWeight = FontWeight(560)
            )
            Text(
                text = stringResource(R.string.setup_complete),
                modifier = Modifier.padding(top = 20.dp),
                color = MiuixTheme.colorScheme.onBackground.copy(alpha = 0.8f)
            )
        }

        Button(
            modifier = Modifier
                .fillMaxWidth()
                .wrapContentHeight()
                .padding(horizontal = 28.dp),
            onClick = {
                onFinish()
            }
        ) {
            Text(
                text = stringResource(R.string.enter_app),
                modifier = Modifier.padding(horizontal = 12.dp),
                fontSize = 18.sp,
                fontWeight = FontWeight.Bold
            )
        }

        Spacer(
            modifier = Modifier
                .navigationBarsPadding()
                .height(16.dp)
        )
    }
}