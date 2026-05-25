package androidx.navigationevent.compose

import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.navigationevent.NavigationEventDispatcher
import androidx.navigationevent.NavigationEventDispatcherOwner

@Composable
fun rememberNavigationEventDispatcherOwner(
    enabled: Boolean = true,
    parent: NavigationEventDispatcherOwner? =
        checkNotNull(LocalNavigationEventDispatcherOwner.current) {
            "No NavigationEventDispatcherOwner provided in LocalNavigationEventDispatcherOwner."
        },
): NavigationEventDispatcherOwner {
    val localDispatcher =
        remember(parent) {
            if (parent != null) {
                NavigationEventDispatcher(parent = parent.navigationEventDispatcher)
            } else {
                NavigationEventDispatcher()
            }
        }

    LaunchedEffect(enabled) { localDispatcher.isEnabled = enabled }
    DisposableEffect(localDispatcher) { onDispose { localDispatcher.dispose() } }

    return remember(localDispatcher) {
        ComposeNavigationEventDispatcherOwner(navigationEventDispatcher = localDispatcher)
    }
}

private class ComposeNavigationEventDispatcherOwner(
    override val navigationEventDispatcher: NavigationEventDispatcher
) : NavigationEventDispatcherOwner
