package androidx.navigationevent.compose

import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.ProvidedValue
import androidx.compose.runtime.compositionLocalOf
import androidx.compose.ui.platform.LocalView
import androidx.navigationevent.NavigationEventDispatcherOwner
import androidx.navigationevent.findViewTreeNavigationEventDispatcherOwner

object LocalNavigationEventDispatcherOwner {
    private val LocalNavigationEventDispatcherOwner =
        compositionLocalOf<NavigationEventDispatcherOwner?> { null }

    val current: NavigationEventDispatcherOwner?
        @Composable
        get() =
            LocalNavigationEventDispatcherOwner.current
                ?: findViewTreeNavigationEventDispatcherOwner()

    infix fun provides(
        navigationEventDispatcherOwner: NavigationEventDispatcherOwner
    ): ProvidedValue<NavigationEventDispatcherOwner?> {
        return LocalNavigationEventDispatcherOwner.provides(navigationEventDispatcherOwner)
    }
}

@Composable
internal fun findViewTreeNavigationEventDispatcherOwner(): NavigationEventDispatcherOwner? =
    LocalView.current.findViewTreeNavigationEventDispatcherOwner()
