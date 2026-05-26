package com.merak.ui.page

import android.widget.Toast
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.merak.ui.components.MiuixBackButton
import com.merak.x.R
import kotlinx.coroutines.launch
import org.koin.androidx.compose.koinViewModel
import top.yukonga.miuix.kmp.basic.ButtonDefaults
import top.yukonga.miuix.kmp.basic.Card
import top.yukonga.miuix.kmp.basic.CircularProgressIndicator
import top.yukonga.miuix.kmp.basic.MiuixScrollBehavior
import top.yukonga.miuix.kmp.basic.Scaffold
import top.yukonga.miuix.kmp.basic.TabRow
import top.yukonga.miuix.kmp.basic.Text
import top.yukonga.miuix.kmp.basic.TextButton
import top.yukonga.miuix.kmp.basic.TextField
import top.yukonga.miuix.kmp.basic.TopAppBar
import top.yukonga.miuix.kmp.basic.rememberTopAppBarState
import top.yukonga.miuix.kmp.utils.overScrollVertical

@Composable
fun ThemeInstallPage(
    onBack: () -> Unit,
    onNavigateToFilePicker: () -> Unit = {},
    viewModel: ThemeInstallViewModel = koinViewModel() // 注入 ViewModel
) {
    val context = LocalContext.current
    val scrollBehavior = MiuixScrollBehavior(rememberTopAppBarState())
    val coroutineScope = rememberCoroutineScope()

    val tabs = listOf(
        stringResource(R.string.tab_local),
        stringResource(R.string.tab_online)
    )
    val pagerState = rememberPagerState(initialPage = 0, pageCount = { tabs.size })
    val selectedTabIndex by remember { derivedStateOf { pagerState.currentPage } }

    // 监听 ViewModel 事件
    LaunchedEffect(Unit) {
        viewModel.installEvent.collect { event ->
            when (event) {
                is InstallEvent.ShowToast -> {
                    Toast.makeText(context, event.message, Toast.LENGTH_SHORT).show()
                }

                is InstallEvent.NavigateBack -> {
                    onBack()
                }
            }
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = stringResource(R.string.home_install_title),
                scrollBehavior = scrollBehavior,
                navigationIcon = {
                    MiuixBackButton(
                        onClick = onBack
                    )
                }
            )
        }
    ) { innerPadding ->
        LazyColumn(
            modifier = Modifier
                .padding(innerPadding)
                .nestedScroll(scrollBehavior.nestedScrollConnection)
                .overScrollVertical()
                .fillMaxSize()
        ) {
            stickyHeader {
                TabRow(
                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 12.dp),
                    tabs = tabs,
                    selectedTabIndex = selectedTabIndex,
                    onTabSelected = { index ->
                        coroutineScope.launch {
                            pagerState.animateScrollToPage(index)
                        }
                    }
                )
            }
            item {
                HorizontalPager(
                    state = pagerState,
                    userScrollEnabled = false,
                    modifier = Modifier.fillMaxSize()
                ) { page ->
                    when (page) {
                        0 -> LocalInstallView(onNavigateToFilePicker)
                        // 将 ViewModel 传递给 OnlineInstallView 或者传递状态和回调
                        1 -> OnlineInstallView(viewModel)
                    }
                }
            }
        }
    }
}

@Composable
fun LocalInstallView(onNavigateToFilePicker: () -> Unit) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 12.dp)
    ) {
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 8.dp)
        ) {
            Text(
                text = stringResource(R.string.local_install_desc),
                modifier = Modifier.padding(16.dp)
            )
        }

        TextButton(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 8.dp),
            colors = ButtonDefaults.textButtonColorsPrimary(),
            text = stringResource(R.string.select_theme_file),
            onClick = onNavigateToFilePicker
        )
    }
}

@Composable
fun OnlineInstallView(viewModel: ThemeInstallViewModel) {
    val context = LocalContext.current
    var url by remember { mutableStateOf("") }
    // 从 ViewModel 获取下载状态
    val isDownloading by viewModel.isDownloading.collectAsState()

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 12.dp)
    ) {
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 8.dp)
        ) {
            Text(
                text = stringResource(R.string.online_install_desc),
                modifier = Modifier.padding(16.dp)
            )
        }

        TextField(
            value = url,
            onValueChange = { url = it },
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 8.dp),
            label = stringResource(R.string.theme_url_label),
            singleLine = true,
            enabled = !isDownloading
        )

        if (isDownloading) {
            CircularProgressIndicator(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 16.dp)
            )
        } else {
            TextButton(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 8.dp),
                colors = ButtonDefaults.textButtonColorsPrimary(),
                text = stringResource(R.string.download_install),
                onClick = {
                    // 所有的逻辑都委托给 ViewModel
                    viewModel.installOnlineTheme(context, url)
                }
            )
        }
    }
}
