package com.peltoche.fyde.layout

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.width
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.Dp
import com.peltoche.fyde.components.content.AppDivider
import com.peltoche.fyde.theme.AppDimensions

/**
 * Master-detail: [primary] and [secondary] side by side once there's room
 * (Medium/Expanded — design-system.md § Responsive names this as exactly
 * where a list+detail split becomes viable), collapsing to one pane at a
 * time on Compact.
 *
 * [secondary] is always required, not nullable — "nothing selected yet" is
 * a real state the caller renders (typically an [com.peltoche.fyde.components.feedback.AppEmptyState]
 * telling the user to pick something), not the absence of a pane. On
 * Compact, [showSecondaryOnCompact] is the one thing the caller's own
 * navigation state controls: false while browsing the primary list, true
 * once something's been selected and the detail should take over the
 * whole screen.
 *
 * [primaryPaneWidth] is fixed, not a proportional weight — an earlier
 * percentage-based split let the list column balloon on wide monitors well
 * past what titles/metadata need, while leaving the secondary pane
 * comparatively squeezed. A fixed list-column width is the master-detail
 * convention most reference UIs (Mail, Files) converge on.
 */
@Composable
fun AppTwoPaneLayout(
    primary: @Composable () -> Unit,
    secondary: @Composable () -> Unit,
    modifier: Modifier = Modifier,
    windowSizeClass: AppWindowSizeClass = LocalAppWindowSizeClass.current,
    showSecondaryOnCompact: Boolean = false,
    primaryPaneWidth: Dp = AppDimensions.twoPaneListWidth,
) {
    if (windowSizeClass == AppWindowSizeClass.Compact) {
        Box(modifier = modifier.fillMaxSize()) {
            if (showSecondaryOnCompact) secondary() else primary()
        }
    } else {
        Row(modifier = modifier.fillMaxSize()) {
            Box(modifier = Modifier.width(primaryPaneWidth).fillMaxHeight()) { primary() }
            AppDivider(vertical = true)
            Box(modifier = Modifier.weight(1f).fillMaxHeight()) { secondary() }
        }
    }
}
