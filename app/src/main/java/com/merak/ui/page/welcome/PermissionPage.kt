package com.merak.ui.page.welcome

import android.Manifest
import android.app.Activity
import android.content.Context
import android.content.Intent
import android.provider.Settings
import android.widget.Toast
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
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.app.ActivityCompat
import androidx.core.net.toUri
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
fun PermissionPage(pagerState: PagerState, viewModel: WelcomeViewModel) {
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()

    val storageGranted by viewModel.storageGranted.collectAsState()
    val accessibilityGranted by viewModel.accessibilityGranted.collectAsState()
    val notificationGranted by viewModel.notificationGranted.collectAsState()

    OnLifecycleEvent(Lifecycle.Event.ON_RESUME) {
        viewModel.checkStandardPermissions(context)
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 24.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Spacer(modifier = Modifier.height(40.dp))

        Icon(
            painter = painterResource(R.drawable.ic_security),
            contentDescription = "Permission",
            tint = MiuixTheme.colorScheme.primary,
            modifier = Modifier.size(90.dp)
        )

        Spacer(modifier = Modifier.height(16.dp))

        Text(
            text = stringResource(R.string.permission_title),
            fontSize = 20.sp,
            fontWeight = FontWeight.SemiBold,
            color = MiuixTheme.colorScheme.onBackground
        )

        Spacer(modifier = Modifier.height(8.dp))

        Text(
            text = stringResource(R.string.permission_desc),
            fontSize = 14.sp,
            textAlign = TextAlign.Center,
            color = MiuixTheme.colorScheme.onSurface.copy(alpha = 0.6f)
        )

        Spacer(modifier = Modifier.height(24.dp))

        PermissionItem(
            icon = painterResource(R.drawable.ic_folder),
            title = stringResource(R.string.permission_storage_title),
            description = stringResource(R.string.permission_storage_desc),
            isChecked = storageGranted,
            onRequest = { requestStoragePermission(context, storageGranted) }
        )

        Spacer(modifier = Modifier.height(12.dp))

        PermissionItem(
            icon = painterResource(R.drawable.ic_accessibility),
            title = stringResource(R.string.permission_accessibility_title),
            description = stringResource(R.string.permission_accessibility_desc),
            isChecked = accessibilityGranted,
            onRequest = { requestAccessibilityPermission(context, accessibilityGranted) }
        )

        Spacer(modifier = Modifier.height(12.dp))

        PermissionItem(
            icon = painterResource(R.drawable.ic_notification),
            title = stringResource(R.string.permission_notification_title),
            description = stringResource(R.string.permission_notification_desc),
            isChecked = notificationGranted,
            onRequest = { requestNotificationPermission(context, notificationGranted) }
        )

        Spacer(modifier = Modifier.weight(1f))

        TextButton(
            text = stringResource(R.string.next_step),
            onClick = {
                coroutineScope.launch {
                    pagerState.animateScrollToPage(4)
                }
            },
            modifier = Modifier.fillMaxWidth(),
            colors = ButtonDefaults.textButtonColorsPrimary()
        )

        Spacer(
            modifier = Modifier
                .navigationBarsPadding()
                .height(16.dp)
        )
    }
}

// Ensure intents remain in the view layer since they interact directly with context and activities
private fun requestStoragePermission(context: Context, granted: Boolean) {
    if (!granted) {
        val intent = Intent(Settings.ACTION_MANAGE_APP_ALL_FILES_ACCESS_PERMISSION).apply {
            data = "package:${context.packageName}".toUri()
        }
        context.startActivity(intent)
    } else {
        Toast.makeText(context, context.getString(R.string.permission_already_granted), Toast.LENGTH_SHORT).show()
    }
}

private fun requestAccessibilityPermission(context: Context, granted: Boolean) {
    if (!granted) {
        val intent = Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS)
        context.startActivity(intent)
    } else {
        Toast.makeText(context, context.getString(R.string.permission_already_granted), Toast.LENGTH_SHORT).show()
    }
}

private fun requestNotificationPermission(context: Context, granted: Boolean) {
    if (granted) {
        Toast.makeText(context, context.getString(R.string.permission_already_granted), Toast.LENGTH_SHORT).show()
    } else {
        ActivityCompat.requestPermissions(
            context as Activity,
            arrayOf(Manifest.permission.POST_NOTIFICATIONS),
            1001
        )
    }
}