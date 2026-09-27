package com.peltoche.fyde.theme

import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

/**
 * Named, cross-cutting sizes from `design/design-system.md` § Shape/Layout —
 * things that are a *size*, not a gap (see [AppSpacing]). Component-internal
 * cosmetic sizing (an icon's own pixel size, a skeleton placeholder's width)
 * stays local to that component rather than living here; only values the
 * spec names or that already repeat identically across files belong in this
 * object.
 */
object AppDimensions {
    /** Shape § Buttons: clears both Android's 48dp and iOS's 44pt touch-target minimums. */
    val touchTargetMin: Dp = 48.dp

    /** Top bar height at [com.peltoche.fyde.layout.AppWindowSizeClass.Compact]. */
    val topBarHeight: Dp = 56.dp

    /** Top bar height at Medium/Expanded — design-system.md's carried-forward Navigation guidance. */
    val topBarHeightExpanded: Dp = 64.dp

    /** Layout § content max width — list/table views, engaged only on very wide desktop viewports. */
    val maxWidthList: Dp = 1280.dp

    /** Layout § content max width — detail/form views (settings, document detail, auth forms). */
    val maxWidthDetail: Dp = 720.dp

    /**
     * `AppTwoPaneLayout`'s primary (list) pane at Medium/Expanded — fixed,
     * not proportional. A percentage split (the original approach) made the
     * list column balloon on wide monitors well past what titles/metadata
     * need, while starving the secondary pane. Matches the list-column width
     * common reference master-detail UIs (Mail, Files) converge on.
     */
    val twoPaneListWidth: Dp = 360.dp

    /** Elevation/Shape — hairline dividers and card edges. */
    val borderHairline: Dp = 1.dp

    /** Focus ring width and the focused-state input indicator. */
    val borderFocus: Dp = 2.dp

    /**
     * Layout § density — list-row minimum heights, applied via
     * `Modifier.heightIn(min = ...)`. These are floors, not targets: a row's
     * *natural* height (icon + two lines of text + default content padding)
     * is already ~60dp, so [rowHeightStandard]/[rowHeightCompact] sitting at
     * or under that verified natural size intentionally add nothing —
     * density comes from Comfortable adding real extra breathing room on
     * touch, not from artificially shrinking content on desktop (a plain
     * `heightIn(min=...)` floor can't shrink below natural size regardless
     * of the value given). Verify against real content before changing —
     * the original 56/48/40dp progression never actually bound at all.
     */
    val rowHeightComfortable: Dp = 72.dp

    /** Layout § density — tablet list rows; at/near natural row height. */
    val rowHeightStandard: Dp = 64.dp

    /** Layout § density — desktop/pointer list rows; at/below natural row height, the tightest tier. */
    val rowHeightCompact: Dp = 56.dp
}
