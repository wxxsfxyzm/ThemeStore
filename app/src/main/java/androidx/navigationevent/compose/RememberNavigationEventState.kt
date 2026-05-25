package androidx.navigationevent.compose

import androidx.compose.runtime.Composable
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.remember
import androidx.navigationevent.NavigationEventInfo

@Composable
fun <T : NavigationEventInfo> rememberNavigationEventState(
    currentInfo: T,
    backInfo: List<T> = emptyList(),
    forwardInfo: List<T> = emptyList(),
): NavigationEventState<T> {
    val state = remember { NavigationEventState(currentInfo, backInfo, forwardInfo) }
    SideEffect {
        state.currentInfo = currentInfo
        state.backInfo = backInfo
        state.forwardInfo = forwardInfo
    }
    return state
}
