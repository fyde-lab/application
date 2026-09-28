package com.peltoche.fyde.layout

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import com.peltoche.fyde.theme.AppDimensions
import com.peltoche.fyde.theme.AppSpacing

/** Which of design-system.md's two content max-widths applies — see § Layout. */
enum class AppContentWidth { List, Detail }

/**
 * The single place "constrained readable content width" + "breakpoint-driven
 * screen margin" lives, so no screen re-derives design-system.md's Layout §
 * numbers by hand. On [AppWindowSizeClass.Compact] the margin is the only
 * thing that does anything — content is already narrower than either
 * max-width — so this fills the mobile "single-column content" requirement
 * for free rather than needing a separate compact-only code path.
 */
@Composable
fun AppContentContainer(
    modifier: Modifier = Modifier,
    width: AppContentWidth = AppContentWidth.Detail,
    windowSizeClass: AppWindowSizeClass = LocalAppWindowSizeClass.current,
    // The full screen margin exists to keep content off a bare window edge —
    // a persistent sidebar (AppNavigationSidebar) already draws that edge,
    // so a caller composed directly next to one passes true for a smaller
    // leading gap instead of stacking the full margin on top of it. The
    // trailing edge keeps its full margin regardless: it's still a bare
    // window edge either way.
    sidebarAdjacent: Boolean = false,
    content: @Composable () -> Unit,
) {
    val maxWidth = when (width) {
        AppContentWidth.List -> AppDimensions.maxWidthList
        AppContentWidth.Detail -> AppDimensions.maxWidthDetail
    }
    val margin = when (windowSizeClass) {
        AppWindowSizeClass.Compact -> AppSpacing.space5
        AppWindowSizeClass.Medium -> AppSpacing.space7
        AppWindowSizeClass.Expanded -> AppSpacing.space8
    }
    val startMargin = if (sidebarAdjacent) AppSpacing.space5 else margin

    // Centering a width-capped box ignores asymmetric padding — on a wide
    // enough viewport the cap kicks in and centers the box regardless of
    // `startMargin`, reopening the full gap `sidebarAdjacent` trims down.
    // Anchoring to the start instead keeps the trimmed gap next to the
    // sidebar and pushes all the leftover width to the trailing edge.
    val alignment = if (sidebarAdjacent) Alignment.TopStart else Alignment.TopCenter

    Box(modifier = modifier.fillMaxWidth(), contentAlignment = alignment) {
        Box(
            modifier = Modifier
                .widthIn(max = maxWidth)
                .fillMaxWidth()
                .padding(start = startMargin, end = margin),
        ) {
            content()
        }
    }
}
