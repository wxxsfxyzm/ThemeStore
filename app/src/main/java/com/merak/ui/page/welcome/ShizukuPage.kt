package com.merak.ui.page.welcome

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.pager.PagerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.Lifecycle
import com.merak.ui.components.OnLifecycleEvent
import com.merak.x.R
import kotlinx.coroutines.launch
import top.yukonga.miuix.kmp.basic.ButtonDefaults
import top.yukonga.miuix.kmp.basic.Icon
import top.yukonga.miuix.kmp.basic.Text
import top.yukonga.miuix.kmp.basic.TextButton
import top.yukonga.miuix.kmp.theme.MiuixTheme

@Composable
fun ShizukuPage(pagerState: PagerState, viewModel: WelcomeViewModel) {
    val coroutineScope = rememberCoroutineScope()
    val isShizukuAvailable by viewModel.isShizukuAvailable.collectAsState()
    val isShizukuGranted by viewModel.isShizukuGranted.collectAsState()

    OnLifecycleEvent(Lifecycle.Event.ON_RESUME) {
        viewModel.checkShizukuStatus()
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 24.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Spacer(modifier = Modifier.height(40.dp))

        Icon(
            painter = painterResource(R.drawable.ic_shizuku),
            contentDescription = "Shizuku",
            tint = MiuixTheme.colorScheme.primary,
            modifier = Modifier.size(90.dp)
        )

        Spacer(modifier = Modifier.height(16.dp))

        Text(
            text = stringResource(R.string.permission_shizuku_title),
            fontSize = 20.sp,
            fontWeight = FontWeight.SemiBold,
            color = MiuixTheme.colorScheme.onBackground
        )

        Spacer(modifier = Modifier.height(8.dp))

        Text(
            text = stringResource(R.string.permission_shizuku_desc),
            fontSize = 14.sp,
            textAlign = TextAlign.Center,
            color = MiuixTheme.colorScheme.onSurface.copy(alpha = 0.6f)
        )

        Spacer(modifier = Modifier.height(32.dp))

        PermissionItem(
            icon = painterResource(R.drawable.ic_shizuku),
            title = stringResource(R.string.permission_shizuku_title),
            description = when {
                isShizukuGranted -> stringResource(R.string.shizuku_activated)
                !isShizukuAvailable -> stringResource(R.string.shizuku_not_available)
                else -> stringResource(R.string.shizuku_authorize)
            },
            isChecked = isShizukuGranted,
            enabled = isShizukuAvailable,
            onRequest = { viewModel.requestShizukuPermission() }
        )

        Spacer(modifier = Modifier.weight(1f))

        TextButton(
            text = if (isShizukuGranted) stringResource(R.string.next_step) else stringResource(R.string.skip),
            onClick = {
                coroutineScope.launch {
                    pagerState.animateScrollToPage(3)
                }
            },
            modifier = Modifier.fillMaxWidth(),
            colors = if (isShizukuGranted) {
                ButtonDefaults.textButtonColorsPrimary()
            } else {
                ButtonDefaults.textButtonColors()
            }
        )

        Spacer(
            modifier = Modifier
                .navigationBarsPadding()
                .height(16.dp)
        )
    }
}