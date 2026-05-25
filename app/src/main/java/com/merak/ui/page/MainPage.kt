package com.merak.ui.page

import android.os.Build
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.res.vectorResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.merak.ui.LocalNavigator
import com.merak.ui.Route
import com.merak.ui.icons.AppIcons
import com.merak.ui.library.FloatingBottomBar
import com.merak.ui.library.FloatingBottomBarItem
import com.merak.ui.page.home.HomePage
import com.merak.ui.page.settings.SettingsPage
import com.merak.ui.theme.getMiuixAppBarColor
import com.merak.ui.theme.rememberMiuixHazeStyle
import com.merak.ui.theme.tsHazeEffect
import com.merak.x.R
import dev.chrisbanes.haze.HazeState
import kotlinx.coroutines.launch
import top.yukonga.miuix.kmp.basic.Icon
import top.yukonga.miuix.kmp.basic.NavigationBar
import top.yukonga.miuix.kmp.basic.NavigationBarItem
import top.yukonga.miuix.kmp.basic.NavigationItem
import top.yukonga.miuix.kmp.basic.NavigationRail
import top.yukonga.miuix.kmp.basic.NavigationRailItem
import top.yukonga.miuix.kmp.basic.Scaffold
import top.yukonga.miuix.kmp.basic.Text
import top.yukonga.miuix.kmp.blur.layerBackdrop
import top.yukonga.miuix.kmp.blur.rememberLayerBackdrop
import top.yukonga.miuix.kmp.theme.MiuixTheme

// Define thresholds to determine screen types
private object UIConstants {
    val WIDE_SCREEN_THRESHOLD = 840.dp
    val MEDIUM_WIDTH_THRESHOLD = 600.dp
    const val PORTRAIT_ASPECT_RATIO_THRESHOLD = 1.2f
}

@Composable
fun MainPage(
    hazeState: HazeState?,
    useFloatingBottomBar: Boolean
) {
    val navigator = LocalNavigator.current
    val pagerState = rememberPagerState(initialPage = 0, pageCount = { 2 })
    val coroutineScope = rememberCoroutineScope()
    val hazeStyle = rememberMiuixHazeStyle()

    val items = listOf(
        NavigationItem(stringResource(R.string.title_home), ImageVector.vectorResource(R.drawable.magic_wand_color)),
        NavigationItem(stringResource(R.string.title_settings), AppIcons.Settings)
    )

    // Use BoxWithConstraints to calculate screen boundaries
    BoxWithConstraints(modifier = Modifier.fillMaxSize()) {
        // Evaluate if the current window is wide enough for a rail
        val isDefinitelyWide = maxWidth > UIConstants.WIDE_SCREEN_THRESHOLD
        val isWideByShape = maxWidth > UIConstants.MEDIUM_WIDTH_THRESHOLD &&
                (maxHeight.value / maxWidth.value < UIConstants.PORTRAIT_ASPECT_RATIO_THRESHOLD)
        val isWideScreen = isDefinitelyWide || isWideByShape

        if (isWideScreen) {
            // Widescreen Layout: Row + NavigationRail
            Row(
                modifier = Modifier
                    .fillMaxSize()
                    .background(MiuixTheme.colorScheme.surface)
            ) {
                NavigationRail(
                    modifier = Modifier
                        .fillMaxHeight()
                        .tsHazeEffect(hazeState, hazeStyle),
                    color = hazeState.getMiuixAppBarColor()
                ) {
                    items.forEachIndexed { index, item ->
                        NavigationRailItem(
                            selected = pagerState.currentPage == index,
                            onClick = {
                                coroutineScope.launch {
                                    pagerState.animateScrollToPage(index)
                                }
                            },
                            icon = item.icon,
                            label = item.label
                        )
                    }
                }

                // Content area takes up the remaining space
                Box(modifier = Modifier.weight(1f)) {
                    HorizontalPager(
                        state = pagerState,
                        modifier = Modifier.fillMaxSize(),
                        pageContent = { page ->
                            when (page) {
                                0 -> HomePage(
                                    onNavigateToThemeInstall = { navigator.push(Route.ThemeInstall) },
                                    onNavigateToLog = { navigator.push(Route.Log) },
                                    hazeState = hazeState,
                                    outerPadding = PaddingValues(0.dp)
                                )

                                1 -> SettingsPage(
                                    onNavigateToAppearance = { navigator.push(Route.Appearance) },
                                    onNavigateToAbout = { navigator.push(Route.About) },
                                    hazeState = hazeState,
                                    outerPadding = PaddingValues(0.dp)
                                )
                            }
                        }
                    )
                }
            }
        } else {
            val floatingBackdrop = rememberLayerBackdrop()
            val useFloatingBottomBarBlur = hazeState != null && Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU

            // Compact Layout: Scaffold + selectable bottom bar
            Scaffold(
                bottomBar = {
                    if (useFloatingBottomBar) {
                        Box(modifier = Modifier.fillMaxWidth()) {
                            FloatingBottomBar(
                                modifier = Modifier
                                    .align(Alignment.BottomCenter)
                                    .padding(
                                        bottom = 8.dp + WindowInsets.navigationBars
                                            .asPaddingValues()
                                            .calculateBottomPadding()
                                    ),
                                selectedIndex = { pagerState.targetPage },
                                onSelected = { index ->
                                    coroutineScope.launch {
                                        pagerState.animateScrollToPage(index)
                                    }
                                },
                                backdrop = floatingBackdrop,
                                tabsCount = items.size,
                                isBlurEnabled = useFloatingBottomBarBlur
                            ) {
                                items.forEachIndexed { index, item ->
                                    FloatingBottomBarItem(
                                        onClick = {
                                            coroutineScope.launch {
                                                pagerState.animateScrollToPage(index)
                                            }
                                        },
                                        modifier = Modifier.defaultMinSize(minWidth = 76.dp)
                                    ) {
                                        Icon(
                                            imageVector = item.icon,
                                            contentDescription = item.label
                                        )
                                        Text(
                                            text = item.label,
                                            fontSize = 11.sp,
                                            lineHeight = 14.sp,
                                            maxLines = 1,
                                            softWrap = false,
                                            overflow = TextOverflow.Visible
                                        )
                                    }
                                }
                            }
                        }
                    } else {
                        NavigationBar(
                            modifier = Modifier.tsHazeEffect(hazeState, hazeStyle),
                            color = hazeState.getMiuixAppBarColor()
                        ) {
                            items.forEachIndexed { index, item ->
                                NavigationBarItem(
                                    selected = pagerState.currentPage == index,
                                    onClick = {
                                        coroutineScope.launch {
                                            pagerState.animateScrollToPage(index)
                                        }
                                    },
                                    icon = item.icon,
                                    label = item.label
                                )
                            }
                        }
                    }
                }
            ) { paddingValues ->
                HorizontalPager(
                    state = pagerState,
                    modifier = Modifier
                        .fillMaxSize()
                        .then(
                            if (useFloatingBottomBar && useFloatingBottomBarBlur) {
                                Modifier.layerBackdrop(floatingBackdrop)
                            } else {
                                Modifier
                            }
                        ),
                    pageContent = { page ->
                        when (page) {
                            0 -> HomePage(
                                onNavigateToThemeInstall = { navigator.push(Route.ThemeInstall) },
                                onNavigateToLog = { navigator.push(Route.Log) },
                                hazeState = hazeState,
                                outerPadding = paddingValues
                            )

                            1 -> SettingsPage(
                                onNavigateToAppearance = { navigator.push(Route.Appearance) },
                                onNavigateToAbout = { navigator.push(Route.About) },
                                hazeState = hazeState,
                                outerPadding = paddingValues
                            )
                        }
                    }
                )
            }
        }
    }
}
