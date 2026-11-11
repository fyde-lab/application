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
     * `AppTwoPaneLayout`'s primary (list) pane at Medium — fixed, not
     * proportional. A percentage split (the original approach) made the
     * list column balloon on wide monitors well past what titles/metadata
     * need, while starving the secondary pane. Matches the list-column width
     * common reference master-detail UIs (Mail, Files) converge on. Kept
     * narrower than [twoPaneListWidthExpanded] because Medium's viewport
     * tops out at 839dp — a wider fixed list column here would leave the
     * detail pane too little room at the low end of that range.
     */
    val twoPaneListWidth: Dp = 360.dp

    /**
     * `AppTwoPaneLayout`'s primary (list) pane at Expanded (>=840dp) —
     * wider than Medium's [twoPaneListWidth]. Expanded always has width to
     * spare, so the list column can show more of a document's title/metadata
     * per row without squeezing the detail pane the way Medium's narrower
     * viewport would. Still a fixed value, not a proportion of window width,
     * for the same reason [twoPaneListWidth] is.
     */
    val twoPaneListWidthExpanded: Dp = 480.dp

    /**
     * `AppIconTile`'s default size — the leading visual of a document row
     * or script card, and the skeleton standing in for it while loading.
     */
    val iconTile: Dp = 44.dp

    /**
     * The `AppIconTile` heading a detail view (document detail, script
     * detail) — larger than a row's [iconTile], since it stands for the item
     * itself, and as tall as the header's two text lines (a 28sp Title line,
     * a `space1` gap, an 18sp Data line) so tile and text share one block.
     * Found via review: the two detail headers used 48dp and 64dp.
     */
    val iconTileHeader: Dp = 48.dp

    /** `AppProgressBar`'s thickness, and the skeleton standing in for one. */
    val progressBarHeight: Dp = 6.dp

    /** One bar of an `AppSegmentedMeter` (the password strength meter). */
    val segmentedMeterHeight: Dp = 4.dp

    /** Elevation/Shape — hairline dividers and card edges. */
    val borderHairline: Dp = 1.dp

    /** Focus ring width and the focused-state input indicator. */
    val borderFocus: Dp = 2.dp

    /** The Fyde logo heading the Login and Register screens. */
    val authLogo: Dp = 56.dp

    /**
     * A small `AppIconTile` beside a line or two of body text — Welcome's
     * promises (`design/assets/screens/O1-bienvenue.png`).
     */
    val iconTileSmall: Dp = 36.dp

    /** A large `AppIconTile` heading a sheet — O4's warning (`tile lg` in the mockups). */
    val iconTileLarge: Dp = 52.dp

    /** A bottom sheet shown as a centred panel (Medium, Expanded, desktop) — DO4b's dialog. */
    val sheetPanelMaxWidth: Dp = 500.dp

    /** D4's source detail panel, flush to the window's right edge beside the table. */
    val sourceDetailPanelWidth: Dp = 380.dp

    /** T1/D1–D6's sidebar beside the content at Expanded. */
    val sidebarWidth: Dp = 260.dp

    /** T2's icon rail beside the content at Medium. */
    val sidebarRailWidth: Dp = 80.dp

    /** One row of the sidebar (a destination, a folder, the seal). */
    val sidebarRowHeight: Dp = 40.dp

    /** One cell of the icon rail, its touch target and selection fill. */
    val sidebarRailCell: Dp = 48.dp

    /** The primary bar marking the selected row or rail cell. */
    val selectionBarWidth: Dp = 3.dp

    /** `FolderCard`'s fixed height (V1, D6): every card in the grid lines up whatever its caption. */
    val folderCardHeight: Dp = 132.dp

    /** The square behind a folder's icon on a `FolderCard`. */
    val folderCardIcon: Dp = 38.dp

    /** `DocumentCard`'s preview well (D1). */
    val documentCardWell: Dp = 160.dp

    /** D2's list column, the folder's documents beside the open one. */
    val documentListColumn: Dp = 400.dp

    /** D2's pass column, between the list and the preview. */
    val documentPassColumn: Dp = 360.dp

    /** The vault's search launcher in the Expanded headers (D1, D6). */
    val searchLauncherWidth: Dp = 300.dp

    /** `AppBackButton`'s circle — design-system.md § Compact: "a 44pt circular back button". */
    val backButton: Dp = 44.dp

    /** `SealHero`'s outer diameter, out to its outermost ring (O1, M2). */
    val sealHero: Dp = 168.dp

    /** The brand panel beside the onboarding and access forms at Expanded (DO1–DO9). */
    val authBrandPanel: Dp = 520.dp

    /** The brand panel narrowed to a band above the onboarding and access forms at Medium. */
    val authBrandBand: Dp = 96.dp

    /** The centred form column of the onboarding and access screens at Medium and Expanded (DO1–DO9). */
    val authFormColumn: Dp = 440.dp

    /** [authFormColumn] widened for the sources (DO7). */
    val authFormColumnWide: Dp = 600.dp

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

/**
 * Glyph sizes — design-system.md § Iconography. The app's glyphs are drawn
 * on a `Canvas` (see `icons/NavGlyphs.kt`) and take their size from the caller's
 * `Modifier.size`, so the scale lives here rather than in each glyph. A
 * size off this scale is an optical exception and says why where it's used.
 */
object AppIconSize {
    /** Inline with caption text or inside a small control: a row's trailing chevron, a button's play mark. */
    val xs: Dp = 12.dp

    /** Leading icon of a field or button, an icon button's glyph. */
    val sm: Dp = 16.dp

    /** Navigation destinations, the glyph inside an `AppIconTile`. */
    val md: Dp = 20.dp

    /**
     * design-system.md's 24 ("tab bar and large tiles", the mockups'
     * `.ic.lg`): the icon rail's destinations, a device's glyph leading a
     * two-line row, the seal card's shield, a sheet's warning.
     */
    val prominent: Dp = 24.dp

    /** The alert glyph inside `AppErrorState`'s circle — drawn edge to edge, so smaller than [xl] to read at the same weight. */
    val lg: Dp = 32.dp

    /** The illustrative glyph inside `AppEmptyState`'s circle. */
    val xl: Dp = 40.dp
}
