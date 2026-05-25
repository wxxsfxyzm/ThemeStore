package androidx.navigationevent.compose

import androidx.compose.runtime.Stable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.navigationevent.NavigationEventHandler
import androidx.navigationevent.NavigationEventInfo
import androidx.navigationevent.NavigationEventTransitionState
import androidx.navigationevent.NavigationEventTransitionState.Idle

@Stable
class NavigationEventState<T : NavigationEventInfo>
internal constructor(
    currentInfo: T,
    backInfo: List<T> = emptyList(),
    forwardInfo: List<T> = emptyList(),
) {
    var transitionState: NavigationEventTransitionState by mutableStateOf(Idle)
    var backInfo: List<T> by mutableStateOf(backInfo)
    var currentInfo: T by mutableStateOf(currentInfo)
    var forwardInfo: List<T> by mutableStateOf(forwardInfo)
    var sourceHandler: NavigationEventHandler<out NavigationEventInfo>? = null
}
