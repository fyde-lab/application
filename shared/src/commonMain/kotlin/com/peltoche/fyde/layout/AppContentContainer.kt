package com.peltoche.fyde.layout

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.Dp
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
    content: @Composable () -> Unit,
) {
    Box(modifier = modifier.fillMaxWidth(), contentAlignment = Alignment.TopCenter) {
        Box(
            modifier = Modifier
                .widthIn(max = contentMaxWidth(width))
                .fillMaxWidth()
                .padding(horizontal = screenMargin(windowSizeClass)),
        ) {
            content()
        }
    }
}

/** design-system.md § Layout's max width for [width]. */
internal fun contentMaxWidth(width: AppContentWidth): Dp = when (width) {
    AppContentWidth.List -> AppDimensions.maxWidthList
    AppContentWidth.Detail -> AppDimensions.maxWidthDetail
}

/** design-system.md § Layout's screen edge margin at [windowSizeClass]. */
internal fun screenMargin(windowSizeClass: AppWindowSizeClass): Dp = when (windowSizeClass) {
    AppWindowSizeClass.Compact -> AppSpacing.space5
    AppWindowSizeClass.Medium -> AppSpacing.space7
    AppWindowSizeClass.Expanded -> AppSpacing.space8
}
