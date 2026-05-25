package androidx.navigationevent.compose

import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.platform.LocalInspectionMode
import androidx.navigationevent.NavigationEvent
import androidx.navigationevent.NavigationEventHandler
import androidx.navigationevent.NavigationEventInfo
import androidx.navigationevent.NavigationEventTransitionState

@Composable
fun NavigationEventHandler(
    state: NavigationEventState<out NavigationEventInfo>,
    isForwardEnabled: Boolean = true,
    onForwardCancelled: (() -> Unit) -> Unit = { callback -> callback() },
    onForwardCompleted: (() -> Unit) -> Unit = { callback -> callback() },
    isBackEnabled: Boolean = true,
    onBackCancelled: (() -> Unit) -> Unit = { callback -> callback() },
    onBackCompleted: (() -> Unit) -> Unit = { callback -> callback() },
) {
    if (LocalInspectionMode.current) return

    val dispatcher =
        checkNotNull(LocalNavigationEventDispatcherOwner.current) {
            "No NavigationEventDispatcher was provided via LocalNavigationEventDispatcherOwner"
        }.navigationEventDispatcher

    val sourceHandler =
        remember(state) {
            ComposeNavigationEventHandler(
                initialInfo = state.currentInfo,
                onTransitionStateChanged = { transitionState ->
                    state.transitionState = transitionState
                },
            )
        }

    SideEffect {
        sourceHandler.isForwardEnabled = isForwardEnabled
        sourceHandler.currentOnForwardCancelled = onForwardCancelled
        sourceHandler.currentOnForwardCompleted = onForwardCompleted
        sourceHandler.isBackEnabled = isBackEnabled
        sourceHandler.currentOnBackCancelled = onBackCancelled
        sourceHandler.currentOnBackCompleted = onBackCompleted
        sourceHandler.setInfo(state.currentInfo, state.backInfo, state.forwardInfo)
    }

    DisposableEffect(state) {
        require(state.sourceHandler == null) {
            "NavigationEventState '$state' is already registered."
        }
        state.sourceHandler = sourceHandler
        dispatcher.addHandler(sourceHandler)
        onDispose {
            sourceHandler.remove()
            state.sourceHandler = null
        }
    }
}

@Composable
fun NavigationBackHandler(
    state: NavigationEventState<out NavigationEventInfo>,
    isBackEnabled: Boolean = true,
    onBackCancelled: (() -> Unit) -> Unit = { callback -> callback() },
    onBackCompleted: (() -> Unit) -> Unit,
) {
    NavigationEventHandler(
        state = state,
        isForwardEnabled = false,
        onForwardCancelled = {},
        onForwardCompleted = {},
        isBackEnabled = isBackEnabled,
        onBackCancelled = onBackCancelled,
        onBackCompleted = onBackCompleted,
    )
}

@Composable
@Suppress("unused")
fun NavigationBackHandler(
    state: NavigationEventState<out NavigationEventInfo>,
    isBackEnabled: Boolean = true,
    onBackCancelled: () -> Unit = {},
    onBackCompleted: () -> Unit,
) {
    NavigationBackHandler(
        state = state,
        isBackEnabled = isBackEnabled,
        onBackCancelled = { callback ->
            callback()
            onBackCancelled()
        },
        onBackCompleted = { callback ->
            callback()
            onBackCompleted()
        },
    )
}

private class ComposeNavigationEventHandler<T : NavigationEventInfo>(
    initialInfo: T,
    private val onTransitionStateChanged: (NavigationEventTransitionState) -> Unit = {},
) : NavigationEventHandler<T>(
    initialInfo = initialInfo,
    isBackEnabled = false,
    isForwardEnabled = false,
) {
    var currentOnForwardCancelled: (() -> Unit) -> Unit = {}
    var currentOnForwardCompleted: (() -> Unit) -> Unit = {}
    var currentOnBackCancelled: (() -> Unit) -> Unit = {}
    var currentOnBackCompleted: (() -> Unit) -> Unit = {}

    override fun onForwardStarted(event: NavigationEvent) {
        onTransitionStateChanged(transitionState)
    }

    override fun onForwardProgressed(event: NavigationEvent) {
        onTransitionStateChanged(transitionState)
    }

    override fun onForwardCancelled() {
        currentOnForwardCancelled { onTransitionStateChanged(transitionState) }
    }

    override fun onForwardCompleted() {
        currentOnForwardCompleted { onTransitionStateChanged(transitionState) }
    }

    override fun onBackStarted(event: NavigationEvent) {
        onTransitionStateChanged(transitionState)
    }

    override fun onBackProgressed(event: NavigationEvent) {
        onTransitionStateChanged(transitionState)
    }

    override fun onBackCancelled() {
        currentOnBackCancelled { onTransitionStateChanged(transitionState) }
    }

    override fun onBackCompleted() {
        currentOnBackCompleted { onTransitionStateChanged(transitionState) }
    }
}
