package com.merak.ui.page.settings.theme

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.tween
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import com.merak.ui.components.MiuixBackButton
import com.merak.ui.components.MiuixSwitchWidget
import com.merak.ui.theme.getMiuixAppBarColor
import com.merak.ui.theme.m3color.RawColor
import com.merak.ui.theme.m3color.ThemeMode
import com.merak.ui.theme.m3color.dynamicColorScheme
import com.merak.ui.theme.rememberMiuixHazeStyle
import com.merak.ui.theme.tsHazeEffect
import com.merak.ui.util.getDisplayName
import com.merak.x.R
import dev.chrisbanes.haze.HazeState
import dev.chrisbanes.haze.hazeSource
import org.koin.androidx.compose.koinViewModel
import top.yukonga.miuix.kmp.basic.Card
import top.yukonga.miuix.kmp.basic.Icon
import top.yukonga.miuix.kmp.basic.MiuixScrollBehavior
import top.yukonga.miuix.kmp.basic.Scaffold
import top.yukonga.miuix.kmp.basic.SmallTitle
import top.yukonga.miuix.kmp.basic.SpinnerEntry
import top.yukonga.miuix.kmp.basic.Text
import top.yukonga.miuix.kmp.basic.TopAppBar
import top.yukonga.miuix.kmp.extra.WindowSpinner
import top.yukonga.miuix.kmp.theme.MiuixTheme
import top.yukonga.miuix.kmp.utils.overScrollVertical
import top.yukonga.miuix.kmp.utils.scrollEndHaptic

@Composable
fun AppearancePage(
    navController: NavController,
    viewModel: AppearanceViewModel = koinViewModel(),
) {
    val state by viewModel.state.collectAsState()
    val scrollBehavior = MiuixScrollBehavior()
    val hazeState = remember { HazeState() }
    val hazeStyle = rememberMiuixHazeStyle()

    Scaffold(
        topBar = {
            TopAppBar(
                modifier = Modifier.tsHazeEffect(hazeState, hazeStyle),
                color = hazeState.getMiuixAppBarColor(),
                title = stringResource(R.string.theme_settings),
                navigationIcon = {
                    MiuixBackButton(modifier = Modifier.padding(start = 16.dp), onClick = { navController.navigateUp() })
                },
                scrollBehavior = scrollBehavior
            )
        }
    ) { paddingValues ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .then(hazeState?.let { Modifier.hazeSource(it) } ?: Modifier)
                .scrollEndHaptic()
                .overScrollVertical()
                .nestedScroll(scrollBehavior.nestedScrollConnection),
            contentPadding = PaddingValues(top = paddingValues.calculateTopPadding()),
            overscrollEffect = null
        ) {
            item { Spacer(modifier = Modifier.size(12.dp)) }
            item { SmallTitle(stringResource(R.string.theme_settings)) }
            item {
                Card(
                    modifier = Modifier
                        .padding(horizontal = 12.dp)
                        .padding(bottom = 12.dp)
                ) {
                    MiuixThemeModeWidget(
                        currentThemeMode = state.themeMode,
                        onThemeModeChange = { newMode ->
                            viewModel.dispatch(AppearanceAction.SetThemeMode(newMode))
                        }
                    )
                    MiuixSwitchWidget(
                        title = stringResource(R.string.theme_settings_miuix_custom_colors),
                        description = stringResource(R.string.theme_settings_miuix_custom_colors_desc),
                        checked = state.useMiuixMonet,
                        onCheckedChange = {
                            viewModel.dispatch(AppearanceAction.SetUseMiuixMonet(it))
                        }
                    )
                    AnimatedVisibility(
                        visible = state.useMiuixMonet,
                        enter = fadeIn() + expandVertically(),
                        exit = fadeOut() + shrinkVertically()
                    ) {
                        MiuixSwitchWidget(
                            title = stringResource(R.string.theme_settings_dynamic_color),
                            description = stringResource(R.string.theme_settings_dynamic_color_desc),
                            checked = state.useDynamicColor,
                            onCheckedChange = {
                                viewModel.dispatch(AppearanceAction.SetUseDynamicColor(it))
                            }
                        )
                    }
                }
            }

            item {
                AnimatedVisibility(
                    visible = state.useMiuixMonet && !state.useDynamicColor,
                    enter = fadeIn(animationSpec = tween(durationMillis = 300, easing = FastOutSlowInEasing)) +
                            expandVertically(animationSpec = tween(durationMillis = 400, easing = FastOutSlowInEasing)),
                    exit = fadeOut(animationSpec = tween(durationMillis = 250, easing = FastOutSlowInEasing)) +
                            shrinkVertically(animationSpec = tween(durationMillis = 350, easing = FastOutSlowInEasing))
                ) {
                    Column {
                        SmallTitle(stringResource(R.string.theme_settings_theme_color))
                        Card(
                            modifier = Modifier
                                .padding(horizontal = 12.dp)
                                .padding(bottom = 12.dp)
                        ) {
                            BoxWithConstraints(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(horizontal = 12.dp, vertical = 16.dp)
                            ) {
                                val itemMinWidth = 88.dp
                                val columns = (this.maxWidth / itemMinWidth).toInt().coerceAtLeast(1)
                                val chunkedColors = state.availableColors.chunked(columns)

                                Column(
                                    modifier = Modifier.fillMaxWidth(),
                                    verticalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    chunkedColors.forEach { rowItems ->
                                        Row(
                                            modifier = Modifier.fillMaxWidth(),
                                            horizontalArrangement = Arrangement.Center
                                        ) {
                                            rowItems.forEach { rawColor ->
                                                Box(
                                                    modifier = Modifier.weight(1f),
                                                    contentAlignment = Alignment.Center
                                                ) {
                                                    ColorSwatchPreview(
                                                        rawColor = rawColor,
                                                        textStyle = MiuixTheme.textStyles.footnote1,
                                                        textColor = MiuixTheme.colorScheme.onSurface,
                                                        isSelected = state.seedColor == rawColor.color && !state.useDynamicColor,
                                                    ) {
                                                        viewModel.dispatch(
                                                            AppearanceAction.SetSeedColor(
                                                                rawColor.color
                                                            )
                                                        )
                                                    }
                                                }
                                            }

                                            val remaining = columns - rowItems.size
                                            if (remaining > 0) {
                                                repeat(remaining) {
                                                    Spacer(Modifier.weight(1f))
                                                }
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }
            item { Spacer(Modifier.navigationBarsPadding()) }
        }
    }
}

/**
 * A SuperSpinner widget for selecting the application's theme mode (Light, Dark, or System).
 *
 * @param modifier The modifier to be applied to the SuperSpinner.
 * @param currentThemeMode The currently selected ThemeMode.
 * @param onThemeModeChange A callback that is invoked when the theme mode selection changes.
 */
@Composable
private fun MiuixThemeModeWidget(
    modifier: Modifier = Modifier,
    currentThemeMode: ThemeMode,
    onThemeModeChange: (ThemeMode) -> Unit,
) {
    val context = LocalContext.current

    // Map of ThemeMode enum to its corresponding string resource ID.
    val themeModeOptions = remember {
        // The order in the map definition determines the order in the spinner.
        mapOf(
            ThemeMode.LIGHT to R.string.theme_settings_theme_mode_light,
            ThemeMode.DARK to R.string.theme_settings_theme_mode_dark,
            ThemeMode.SYSTEM to R.string.theme_settings_theme_mode_system
        )
    }

    // Convert the map of options to a list of SpinnerEntry for the SuperSpinner component.
    // The order of items in the list is important for index mapping.
    val spinnerEntries = remember(themeModeOptions) {
        themeModeOptions.entries.map { entry ->
            SpinnerEntry(title = context.getString(entry.value))
        }
    }

    // Calculate the selected index based on the current theme mode.
    // It finds the index of the currentThemeMode in the ordered list of keys.
    val selectedIndex = remember(currentThemeMode, themeModeOptions) {
        themeModeOptions.keys.indexOf(currentThemeMode).coerceAtLeast(0)
    }

    WindowSpinner(
        modifier = modifier,
        title = stringResource(id = R.string.theme_settings_theme_mode),
        items = spinnerEntries,
        selectedIndex = selectedIndex,
        onSelectedIndexChange = { newIndex ->
            // Retrieve the new ThemeMode based on the selected index.
            val newMode = themeModeOptions.keys.elementAt(newIndex)
            // Invoke the callback only if the mode has actually changed.
            if (currentThemeMode != newMode) {
                onThemeModeChange(newMode)
            }
        }
    )
}

@Composable
private fun ColorSwatchPreview(
    rawColor: RawColor,
    isSelected: Boolean,
    textStyle: TextStyle,
    textColor: Color,
    onClick: () -> Unit
) {
    val isDarkForPreview = false
    val scheme = remember(rawColor.color, isDarkForPreview) {
        dynamicColorScheme(
            keyColor = rawColor.color,
            isDark = isDarkForPreview
        )
    }

    val primaryForSwatch = scheme.primaryContainer.copy(alpha = 0.9f)
    val secondaryForSwatch = scheme.secondaryContainer.copy(alpha = 0.6f)
    val tertiaryForSwatch = scheme.tertiaryContainer.copy(alpha = 0.9f)

    val squircleBackgroundColor = scheme.primary.copy(alpha = 0.3f)

    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier
            .clip(RoundedCornerShape(20.dp))
            .clickable(onClick = onClick)
            .padding(vertical = 8.dp)
    ) {
        Box(
            modifier = Modifier
                .size(64.dp)
                .background(color = squircleBackgroundColor, shape = RoundedCornerShape(16.dp)),
            contentAlignment = Alignment.Center
        ) {
            Box(
                modifier = Modifier
                    .size(48.dp)
                    .clip(CircleShape),
                contentAlignment = Alignment.Center
            ) {
                Canvas(modifier = Modifier.fillMaxSize()) {
                    drawArc(
                        color = primaryForSwatch,
                        startAngle = 180f,
                        sweepAngle = 180f,
                        useCenter = true
                    )
                    drawArc(
                        color = tertiaryForSwatch,
                        startAngle = 90f,
                        sweepAngle = 90f,
                        useCenter = true
                    )
                    drawArc(
                        color = secondaryForSwatch,
                        startAngle = 0f,
                        sweepAngle = 90f,
                        useCenter = true
                    )
                }

                Box(
                    modifier = Modifier
                        .size(26.dp)
                        .clip(CircleShape)
                        .background(scheme.primary),
                    contentAlignment = Alignment.Center
                ) {
                    if (isSelected) {
                        Icon(
                            imageVector = Icons.Default.Check,
                            contentDescription = "Selected",
                            tint = scheme.inversePrimary,
                            modifier = Modifier.size(16.dp)
                        )
                    }
                }
            }
        }

        if (rawColor.getDisplayName() !== rawColor.key) {
            Spacer(Modifier.height(12.dp))
            Text(
                text = rawColor.getDisplayName(),
                style = textStyle,
                color = textColor,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }
    }
}