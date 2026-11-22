package com.peltoche.fyde.components.navigation

import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.navigationevent.NavigationEventInfo
import androidx.navigationevent.compose.NavigationBackHandler
import androidx.navigationevent.compose.rememberNavigationEventState
import com.peltoche.fyde.getPlatform

/**
 * Runs [onBack] on the platform's back gesture: Android's system back
 * (the edge swipe or the back button) and iOS's swipe from the screen's
 * edge. Every on-screen back control calls it with its own action, so the
 * gesture always does what the visible back button does.
 *
 * While several are composed, the last one composed takes the gesture (D2's
 * open document before the list beside it). Where none is active, Android's
 * back leaves the app and iOS's edge swipe does nothing.
 *
 * Only where [com.peltoche.fyde.Platform.hasSystemBack]: on desktop and the
 * web Compose maps Esc to back, which already closes sheets and dialogs there.
 */
@Composable
fun SystemBackHandler(onBack: () -> Unit) {
    val hasSystemBack = remember { getPlatform().hasSystemBack }
    if (!hasSystemBack) return
    NavigationBackHandler(
        state = rememberNavigationEventState(NavigationEventInfo.None),
        onBackCompleted = onBack,
    )
}
