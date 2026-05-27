package com.merak.ui.page

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
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
import androidx.compose.foundation.pager.PagerState
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.RectangleShape
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
import com.merak.ui.page.accessibility.AccessibilityPage
import com.merak.ui.page.home.HomePage
import com.merak.ui.page.settings.SettingsPage
import com.merak.ui.theme.rememberMiuixBlurBackdrop
import com.merak.x.R
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch
import top.yukonga.miuix.kmp.basic.Icon
import top.yukonga.miuix.kmp.basic.NavigationBar
import top.yukonga.miuix.kmp.basic.NavigationBarItem
import top.yukonga.miuix.kmp.basic.NavigationItem
import top.yukonga.miuix.kmp.basic.NavigationRail
import top.yukonga.miuix.kmp.basic.NavigationRailItem
import top.yukonga.miuix.kmp.basic.Scaffold
import top.yukonga.miuix.kmp.basic.Text
import top.yukonga.miuix.kmp.blur.BlendColorEntry
import top.yukonga.miuix.kmp.blur.BlurColors
import top.yukonga.miuix.kmp.blur.LayerBackdrop
import top.yukonga.miuix.kmp.blur.layerBackdrop
import top.yukonga.miuix.kmp.blur.rememberLayerBackdrop
import top.yukonga.miuix.kmp.blur.textureBlur
import top.yukonga.miuix.kmp.theme.MiuixTheme

private object UIConstants {
    val WIDE_SCREEN_THRESHOLD = 840.dp
    val MEDIUM_WIDTH_THRESHOLD = 600.dp
    const val PORTRAIT_ASPECT_RATIO_THRESHOLD = 1.2f
}

@Composable
fun MainPage(
    useBlur: Boolean,
    useFloatingBottomBar: Boolean
) {
    val homeLabel = stringResource(R.string.title_home)
    val accessibilityLabel = stringResource(R.string.title_accessibility_manager)
    val settingsLabel = stringResource(R.string.title_settings)
    val homeIcon = ImageVector.vectorResource(R.drawable.magic_wand_color)
    val accessibilityIcon = ImageVector.vectorResource(R.drawable.ic_accessibility)
    val items = remember(homeLabel, accessibilityLabel, settingsLabel, homeIcon, accessibilityIcon) {
        listOf(
            NavigationItem(homeLabel, homeIcon),
            NavigationItem(accessibilityLabel, accessibilityIcon),
            NavigationItem(settingsLabel, AppIcons.Settings)
        )
    }

    val pagerState = rememberPagerState(initialPage = 0, pageCount = { items.size })
    val coroutineScope = rememberCoroutineScope()
    val floatingBackdrop = rememberLayerBackdrop()
    val miuixBackdrop = rememberMiuixBlurBackdrop(useBlur)

    BoxWithConstraints(modifier = Modifier.fillMaxSize()) {
        val isDefinitelyWide = maxWidth > UIConstants.WIDE_SCREEN_THRESHOLD
        val isWideByShape = maxWidth > UIConstants.MEDIUM_WIDTH_THRESHOLD &&
                (maxHeight.value / maxWidth.value < UIConstants.PORTRAIT_ASPECT_RATIO_THRESHOLD)

        if (isDefinitelyWide || isWideByShape) {
            MainWideScreenLayout(
                pagerState = pagerState,
                coroutineScope = coroutineScope,
                navigationItems = items,
                useFloatingBottomBar = useFloatingBottomBar,
                useFloatingBottomBarBlur = useBlur,
                floatingBackdrop = floatingBackdrop,
                miuixBackdrop = miuixBackdrop
            )
        } else {
            MainCompactLayout(
                pagerState = pagerState,
                coroutineScope = coroutineScope,
                navigationItems = items,
                useFloatingBottomBar = useFloatingBottomBar,
                useFloatingBottomBarBlur = useBlur,
                floatingBackdrop = floatingBackdrop,
                miuixBackdrop = miuixBackdrop
            )
        }
    }
}

@Composable
private fun MainFloatingBottomBar(
    pagerState: PagerState,
    coroutineScope: CoroutineScope,
    navigationItems: List<NavigationItem>,
    useFloatingBottomBarBlur: Boolean,
    floatingBackdrop: LayerBackdrop
) {
    Box(modifier = Modifier.fillMaxWidth()) {
        FloatingBottomBar(
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .clickable(
                    interactionSource = remember { MutableInteractionSource() },
                    indication = null,
                    onClick = {},
                )
                .padding(
                    bottom = 12.dp + WindowInsets.navigationBars.asPaddingValues()
                        .calculateBottomPadding()
                ),
            selectedIndex = { pagerState.targetPage },
            onSelected = { index ->
                coroutineScope.launch {
                    pagerState.animateScrollToPage(index)
                }
            },
            backdrop = floatingBackdrop,
            tabsCount = navigationItems.size,
            isBlurEnabled = useFloatingBottomBarBlur
        ) {
            navigationItems.forEachIndexed { index, item ->
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
}

@Composable
private fun MainCompactLayout(
    pagerState: PagerState,
    coroutineScope: CoroutineScope,
    navigationItems: List<NavigationItem>,
    useFloatingBottomBar: Boolean,
    useFloatingBottomBarBlur: Boolean,
    floatingBackdrop: LayerBackdrop?,
    miuixBackdrop: LayerBackdrop?
) {
    Scaffold(
        modifier = Modifier.fillMaxSize(),
        bottomBar = {
            if (useFloatingBottomBar && floatingBackdrop != null) {
                MainFloatingBottomBar(
                    pagerState = pagerState,
                    coroutineScope = coroutineScope,
                    navigationItems = navigationItems,
                    useFloatingBottomBarBlur = useFloatingBottomBarBlur,
                    floatingBackdrop = floatingBackdrop
                )
            } else if (!useFloatingBottomBar) {
                MainNavigationBar(
                    pagerState = pagerState,
                    coroutineScope = coroutineScope,
                    navigationItems = navigationItems,
                    miuixBackdrop = miuixBackdrop
                )
            }
        }
    ) { paddingValues ->
        MainPagerContent(
            pagerState = pagerState,
            modifier = Modifier.fillMaxSize(),
            outerPadding = paddingValues,
            useFloatingBottomBar = useFloatingBottomBar,
            floatingBackdrop = floatingBackdrop,
            miuixBackdrop = miuixBackdrop
        )
    }
}

@Composable
private fun MainWideScreenLayout(
    pagerState: PagerState,
    coroutineScope: CoroutineScope,
    navigationItems: List<NavigationItem>,
    useFloatingBottomBar: Boolean,
    useFloatingBottomBarBlur: Boolean,
    floatingBackdrop: LayerBackdrop?,
    miuixBackdrop: LayerBackdrop?
) {
    if (useFloatingBottomBar) {
        MainWideContent(
            pagerState = pagerState,
            navigationItems = navigationItems,
            useFloatingBottomBar = true,
            useFloatingBottomBarBlur = useFloatingBottomBarBlur,
            floatingBackdrop = floatingBackdrop,
            miuixBackdrop = miuixBackdrop
        )
    } else {
        Row(
            modifier = Modifier
                .fillMaxSize()
                .background(MiuixTheme.colorScheme.surface)
        ) {
            MainNavigationRail(
                pagerState = pagerState,
                coroutineScope = coroutineScope,
                navigationItems = navigationItems,
                miuixBackdrop = miuixBackdrop
            )

            Box(modifier = Modifier.weight(1f)) {
                MainWideContent(
                    pagerState = pagerState,
                    navigationItems = navigationItems,
                    useFloatingBottomBar = false,
                    useFloatingBottomBarBlur = false,
                    floatingBackdrop = floatingBackdrop,
                    miuixBackdrop = miuixBackdrop
                )
            }
        }
    }
}

@Composable
private fun MainWideContent(
    pagerState: PagerState,
    navigationItems: List<NavigationItem>,
    useFloatingBottomBar: Boolean,
    useFloatingBottomBarBlur: Boolean,
    floatingBackdrop: LayerBackdrop?,
    miuixBackdrop: LayerBackdrop?
) {
    val coroutineScope = rememberCoroutineScope()
    Scaffold(
        modifier = Modifier.fillMaxSize(),
        bottomBar = {
            if (useFloatingBottomBar && floatingBackdrop != null) {
                MainFloatingBottomBar(
                    pagerState = pagerState,
                    coroutineScope = coroutineScope,
                    navigationItems = navigationItems,
                    useFloatingBottomBarBlur = useFloatingBottomBarBlur,
                    floatingBackdrop = floatingBackdrop
                )
            }
        }
    ) { paddingValues ->
        MainPagerContent(
            pagerState = pagerState,
            modifier = Modifier.fillMaxSize(),
            outerPadding = paddingValues,
            useFloatingBottomBar = useFloatingBottomBar,
            floatingBackdrop = floatingBackdrop,
            miuixBackdrop = miuixBackdrop
        )
    }
}

@Composable
private fun MainNavigationBar(
    pagerState: PagerState,
    coroutineScope: CoroutineScope,
    navigationItems: List<NavigationItem>,
    miuixBackdrop: LayerBackdrop?
) {
    val blurActive = miuixBackdrop != null
    val barColor = if (blurActive) Color.Transparent else MiuixTheme.colorScheme.surface

    Box(
        modifier = Modifier
            .then(miuixBackdrop?.let { Modifier.miuixBarBlur(it) } ?: Modifier)
            .background(barColor)
    ) {
        NavigationBar(color = barColor) {
            navigationItems.forEachIndexed { index, item ->
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

@Composable
private fun MainNavigationRail(
    pagerState: PagerState,
    coroutineScope: CoroutineScope,
    navigationItems: List<NavigationItem>,
    miuixBackdrop: LayerBackdrop?
) {
    val blurActive = miuixBackdrop != null
    val railColor = if (blurActive) Color.Transparent else MiuixTheme.colorScheme.surface

    Box(
        modifier = Modifier
            .fillMaxHeight()
            .then(miuixBackdrop?.let { Modifier.miuixBarBlur(it) } ?: Modifier)
            .background(railColor)
    ) {
        NavigationRail(
            modifier = Modifier.fillMaxHeight(),
            color = railColor
        ) {
            navigationItems.forEachIndexed { index, item ->
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
    }
}

@Composable
private fun MainPagerContent(
    pagerState: PagerState,
    modifier: Modifier = Modifier,
    outerPadding: PaddingValues,
    useFloatingBottomBar: Boolean,
    floatingBackdrop: LayerBackdrop?,
    miuixBackdrop: LayerBackdrop?
) {
    val navigator = LocalNavigator.current
    val enablePageBlur = miuixBackdrop != null

    HorizontalPager(
        state = pagerState,
        overscrollEffect = null,
        modifier = modifier
            .then(
                if (useFloatingBottomBar && floatingBackdrop != null) {
                    Modifier.layerBackdrop(floatingBackdrop)
                } else {
                    Modifier
                }
            )
            .then(
                if (!useFloatingBottomBar && miuixBackdrop != null) {
                    Modifier.layerBackdrop(miuixBackdrop)
                } else {
                    Modifier
                }
            )
    ) { page ->
        when (page) {
            0 -> HomePage(
                onNavigateToThemeInstall = { navigator.push(Route.ThemeInstall) },
                onNavigateToLog = { navigator.push(Route.Log) },
                enableBlur = enablePageBlur,
                outerPadding = outerPadding
            )

            1 -> AccessibilityPage(
                enableBlur = enablePageBlur,
                outerPadding = outerPadding
            )

            2 -> SettingsPage(
                onNavigateToAppearance = { navigator.push(Route.Appearance) },
                onNavigateToAbout = { navigator.push(Route.About) },
                enableBlur = enablePageBlur,
                outerPadding = outerPadding
            )
        }
    }
}

@Composable
private fun Modifier.miuixBarBlur(backdrop: LayerBackdrop): Modifier =
    textureBlur(
        backdrop = backdrop,
        shape = RectangleShape,
        blurRadius = 25f,
        colors = BlurColors(
            blendColors = listOf(
                BlendColorEntry(color = MiuixTheme.colorScheme.surface.copy(0.8f)),
            ),
        ),
    )
