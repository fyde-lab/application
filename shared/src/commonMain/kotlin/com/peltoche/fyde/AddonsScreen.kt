package com.peltoche.fyde

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.peltoche.fyde.components.content.AppBadge
import com.peltoche.fyde.components.content.AppBadgeVariant
import com.peltoche.fyde.components.content.AppChip
import com.peltoche.fyde.components.content.AppDivider
import com.peltoche.fyde.components.content.AppListItem
import com.peltoche.fyde.components.feedback.AppBanner
import com.peltoche.fyde.components.feedback.AppEmptyState
import com.peltoche.fyde.components.feedback.AppErrorState
import com.peltoche.fyde.components.feedback.AppLoading
import com.peltoche.fyde.components.feedback.AppSkeleton
import com.peltoche.fyde.components.form.AppSearchField
import com.peltoche.fyde.components.navigation.AppTopBar
import com.peltoche.fyde.layout.AppContentContainer
import com.peltoche.fyde.layout.AppContentWidth
import com.peltoche.fyde.layout.AppTwoPaneLayout
import com.peltoche.fyde.layout.AppWindowSizeClass
import com.peltoche.fyde.layout.LocalAppWindowSizeClass
import com.peltoche.fyde.theme.AppColors
import com.peltoche.fyde.theme.AppDimensions
import com.peltoche.fyde.theme.AppRadius
import com.peltoche.fyde.theme.AppSpacing
import com.peltoche.fyde.resources.Res
import com.peltoche.fyde.resources.addon_installed_badge
import com.peltoche.fyde.resources.addon_select_description
import com.peltoche.fyde.resources.addon_select_title
import com.peltoche.fyde.resources.addons_available_section
import com.peltoche.fyde.resources.addons_empty_description
import com.peltoche.fyde.resources.addons_empty_title
import com.peltoche.fyde.resources.addons_filter_all
import com.peltoche.fyde.resources.addons_installed_section
import com.peltoche.fyde.resources.addons_no_match
import com.peltoche.fyde.resources.addons_none_available_description
import com.peltoche.fyde.resources.addons_none_available_title
import com.peltoche.fyde.resources.addons_none_installed_description
import com.peltoche.fyde.resources.addons_none_installed_title
import com.peltoche.fyde.resources.addons_search_placeholder
import com.peltoche.fyde.resources.addons_show_all
import com.peltoche.fyde.resources.addons_title
import com.peltoche.fyde.resources.clear_search
import com.peltoche.fyde.resources.connect_failed_title
import com.peltoche.fyde.resources.connecting_message
import com.peltoche.fyde.resources.dismiss
import com.peltoche.fyde.resources.no_matches_title
import com.peltoche.fyde.resources.retry
import com.peltoche.fyde.resources.script_version
import org.jetbrains.compose.resources.stringResource

/**
 * Everything [AddonsScreen] needs to render, and nothing it doesn't — no
 * `FydeSdk` types appear here beyond [FydeScript] itself, which is already
 * SDK-free (see its own doc comment). The caller (see `AddonsScreenContainer`
 * in `App.kt`) owns the SDK calls and maps their results into this state,
 * same split as [DocumentListUiState]/[ScriptListUiState].
 */
sealed interface AddonsUiState {
    /** Not yet known whether the server is reachable; nothing to show at all. */
    data object Connecting : AddonsUiState

    /** The initial connection failed outright. */
    data class ConnectFailed(val message: String, val onRetry: () -> Unit) : AddonsUiState

    /**
     * Connected. [scripts] is `null` while the first page is still being
     * fetched (known shape, unknown content — rendered as skeleton rows
     * rather than a spinner), and non-null once loaded (possibly empty).
     * [enabledScriptIds] marks which of [scripts] the authenticated user
     * currently has installed — badged in the list, sorted ahead of every
     * other public script, and what the Installed/Available filter splits
     * on. [pendingScriptIds] marks a script whose install/uninstall call is
     * in flight, so a second click can't race the first. [banner] surfaces a
     * non-fatal problem (an install/uninstall call failing) without
     * discarding what's already loaded.
     */
    data class Loaded(
        val scripts: List<FydeScript>?,
        val enabledScriptIds: Set<String> = emptySet(),
        val pendingScriptIds: Set<String> = emptySet(),
        val banner: String? = null,
        val onDismissBanner: () -> Unit = {},
    ) : AddonsUiState
}

/**
 * The Add-ons destination: a searchable, filterable list of public scripts
 * beside a detail pane — the same master-detail shape as
 * [DocumentListScreen], per design-system.md § Responsive behavior (a list
 * + detail split from Medium up, one pane at a time on Compact).
 *
 * [selectedScriptId] is hoisted to the caller rather than kept local (unlike
 * the document list's preview selection): on Compact the caller swaps this
 * whole screen for the [AddonDetailScreen] drill-down instead, and keeping
 * one selection for both means resizing the window across the breakpoint
 * keeps the same add-on open rather than dropping it.
 *
 * Replaces an earlier card grid. Found via redesign review: cards showed
 * only a name and a two-line blurb, so comparing add-ons or seeing what was
 * installed meant opening each one; Installed/Available were two grid
 * sections with no way to narrow to either; there was no search; and every
 * install/uninstall meant a full-screen round trip away from the list. A
 * dense list with an Installed badge, search, single-choice filter chips,
 * and an in-place detail pane fixes all four at once, and matches the rest
 * of the app's list screens instead of being the one grid.
 */
@Composable
fun AddonsScreen(
    state: AddonsUiState,
    modifier: Modifier = Modifier,
    selectedScriptId: String? = null,
    onSelect: (FydeScript) -> Unit = {},
    onInstall: (FydeScript) -> Unit = {},
    onUninstall: (FydeScript) -> Unit = {},
    onOpenDocuments: () -> Unit = {},
    onOpenScripts: () -> Unit = {},
    onOpenSettings: () -> Unit = {},
) {
    when (state) {
        AddonsUiState.Connecting -> AppLoading(
            modifier = modifier,
            message = stringResource(Res.string.connecting_message),
        )
        is AddonsUiState.ConnectFailed -> AppErrorState(
            modifier = modifier,
            icon = { AlertGlyph(modifier = Modifier.size(32.dp)) },
            title = stringResource(Res.string.connect_failed_title),
            description = state.message,
            actionLabel = stringResource(Res.string.retry),
            onAction = state.onRetry,
        )
        is AddonsUiState.Loaded -> LoadedAddons(
            state = state,
            modifier = modifier,
            selectedScriptId = selectedScriptId,
            onSelect = onSelect,
            onInstall = onInstall,
            onUninstall = onUninstall,
            onOpenDocuments = onOpenDocuments,
            onOpenScripts = onOpenScripts,
            onOpenSettings = onOpenSettings,
        )
    }
}

/** The list's single-choice facet — presentation-only, so it stays local like the search query. */
private enum class AddonFilter { All, Installed, Available }

@Composable
private fun LoadedAddons(
    state: AddonsUiState.Loaded,
    modifier: Modifier,
    selectedScriptId: String?,
    onSelect: (FydeScript) -> Unit,
    onInstall: (FydeScript) -> Unit,
    onUninstall: (FydeScript) -> Unit,
    onOpenDocuments: () -> Unit,
    onOpenScripts: () -> Unit,
    onOpenSettings: () -> Unit,
) {
    var searchQuery by remember { mutableStateOf("") }
    var filter by remember { mutableStateOf(AddonFilter.All) }
    val windowSizeClass = LocalAppWindowSizeClass.current
    val isCompact = windowSizeClass == AppWindowSizeClass.Compact
    val isDesktop = windowSizeClass == AppWindowSizeClass.Expanded

    TopLevelScaffold(
        current = SidebarDestination.Addons,
        onOpenDocuments = onOpenDocuments,
        onOpenScripts = onOpenScripts,
        onOpenAddons = {},
        onOpenSettings = onOpenSettings,
        modifier = modifier,
        topBar = { AppTopBar(title = stringResource(Res.string.addons_title), windowSizeClass = windowSizeClass) },
    ) { contentPadding ->
        AppContentContainer(
            modifier = Modifier
                .fillMaxSize()
                .padding(contentPadding)
                .background(AppColors.background),
            width = AppContentWidth.List,
            windowSizeClass = windowSizeClass,
            sidebarAdjacent = isDesktop,
        ) {
            Column(modifier = Modifier.fillMaxSize()) {
                state.banner?.let { message ->
                    AppBanner(message = message, onDismiss = state.onDismissBanner, dismissLabel = stringResource(Res.string.dismiss))
                }

                val scripts = state.scripts
                // Installed first, then by name — the server's own order
                // (`list_public_scripts`, last-updated) isn't a useful
                // browsing order for a list the user scans by name.
                val sorted = remember(scripts, state.enabledScriptIds) {
                    scripts.orEmpty().sortedWith(
                        compareBy<FydeScript> { it.id !in state.enabledScriptIds }.thenBy { it.name.lowercase() },
                    )
                }
                val visible = remember(sorted, state.enabledScriptIds, filter, searchQuery) {
                    sorted.filter { script ->
                        val installed = script.id in state.enabledScriptIds
                        val inFilter = when (filter) {
                            AddonFilter.All -> true
                            AddonFilter.Installed -> installed
                            AddonFilter.Available -> !installed
                        }
                        inFilter && script.matches(searchQuery)
                    }
                }

                AppTwoPaneLayout(
                    modifier = Modifier.weight(1f),
                    windowSizeClass = windowSizeClass,
                    primaryPaneWidth = if (isDesktop) AppDimensions.twoPaneListWidthExpanded else AppDimensions.twoPaneListWidth,
                    primary = {
                        // Trailing padding mirrors the container's leading
                        // margin so the list column doesn't run flush into
                        // the divider (design-system.md § List spacing); on
                        // Compact there's no divider, and the container's own
                        // margin already covers both edges.
                        Column(modifier = Modifier.fillMaxSize().padding(end = if (isCompact) 0.dp else AppSpacing.space5)) {
                            Spacer(Modifier.height(AppSpacing.space3))
                            AppSearchField(
                                value = searchQuery,
                                onValueChange = { searchQuery = it },
                                placeholder = stringResource(Res.string.addons_search_placeholder),
                                onClear = { searchQuery = "" },
                                clearContentDescription = stringResource(Res.string.clear_search),
                                leadingIcon = { SearchGlyph(modifier = Modifier.size(16.dp)) },
                                enabled = !scripts.isNullOrEmpty(),
                            )

                            if (!scripts.isNullOrEmpty()) {
                                Spacer(Modifier.height(AppSpacing.space3))
                                val installedCount = scripts.count { it.id in state.enabledScriptIds }
                                FilterRow(
                                    current = filter,
                                    allCount = scripts.size,
                                    installedCount = installedCount,
                                    availableCount = scripts.size - installedCount,
                                    onSelect = { filter = it },
                                )
                            }
                            Spacer(Modifier.height(AppSpacing.space2))

                            when {
                                scripts == null -> AddonSkeletonList(modifier = Modifier.weight(1f))

                                scripts.isEmpty() -> AppEmptyState(
                                    modifier = Modifier.weight(1f),
                                    icon = { PuzzleGlyph(modifier = Modifier.size(40.dp), tint = AppColors.textMuted, strokeWidth = 2.dp) },
                                    title = stringResource(Res.string.addons_empty_title),
                                    description = stringResource(Res.string.addons_empty_description),
                                )

                                visible.isEmpty() && searchQuery.isNotBlank() -> AppEmptyState(
                                    modifier = Modifier.weight(1f),
                                    icon = { SearchGlyph(modifier = Modifier.size(40.dp), strokeWidth = 2.dp) },
                                    title = stringResource(Res.string.no_matches_title),
                                    description = stringResource(Res.string.addons_no_match, searchQuery),
                                    actionLabel = stringResource(Res.string.clear_search),
                                    onAction = { searchQuery = "" },
                                )

                                visible.isEmpty() -> AppEmptyState(
                                    modifier = Modifier.weight(1f),
                                    icon = { PuzzleGlyph(modifier = Modifier.size(40.dp), tint = AppColors.textMuted, strokeWidth = 2.dp) },
                                    title = stringResource(
                                        if (filter == AddonFilter.Installed) Res.string.addons_none_installed_title else Res.string.addons_none_available_title,
                                    ),
                                    description = stringResource(
                                        if (filter == AddonFilter.Installed) {
                                            Res.string.addons_none_installed_description
                                        } else {
                                            Res.string.addons_none_available_description
                                        },
                                    ),
                                    actionLabel = stringResource(Res.string.addons_show_all),
                                    onAction = { filter = AddonFilter.All },
                                )

                                else -> AddonList(
                                    scripts = visible,
                                    enabledScriptIds = state.enabledScriptIds,
                                    // Compact never shows a selection: picking a
                                    // row there opens the drill-down instead.
                                    selectedScriptId = if (isCompact) null else selectedScriptId,
                                    rowMinHeight = rowMinHeightFor(windowSizeClass),
                                    onSelect = onSelect,
                                    modifier = Modifier.weight(1f),
                                )
                            }
                        }
                    },
                    secondary = {
                        val selected = scripts?.find { it.id == selectedScriptId }
                        if (selected == null) {
                            AppEmptyState(
                                icon = { PuzzleGlyph(modifier = Modifier.size(40.dp), tint = AppColors.textMuted, strokeWidth = 2.dp) },
                                title = stringResource(Res.string.addon_select_title),
                                description = stringResource(Res.string.addon_select_description),
                            )
                        } else {
                            Column(modifier = Modifier.fillMaxSize().verticalScroll(rememberScrollState())) {
                                AddonDetailContent(
                                    script = selected,
                                    isInstalled = selected.id in state.enabledScriptIds,
                                    isPending = selected.id in state.pendingScriptIds,
                                    onInstall = { onInstall(selected) },
                                    onUninstall = { onUninstall(selected) },
                                    horizontalPadding = AppSpacing.space5,
                                )
                            }
                        }
                    },
                )
            }
        }
    }
}

/** Name, tagline, or long description — whatever the user might remember an add-on by. */
private fun FydeScript.matches(query: String): Boolean {
    if (query.isBlank()) return true
    val needle = query.trim()
    return name.contains(needle, ignoreCase = true) ||
        shortDescription.contains(needle, ignoreCase = true) ||
        description.contains(needle, ignoreCase = true)
}

/**
 * All / Installed / Available, each with its count, as single-choice
 * [AppChip]s. Wraps (FlowRow) rather than overflowing: the list column is a
 * fixed 360dp at Medium, and translated labels with counts can outgrow it.
 */
@Composable
private fun FilterRow(
    current: AddonFilter,
    allCount: Int,
    installedCount: Int,
    availableCount: Int,
    onSelect: (AddonFilter) -> Unit,
) {
    FlowRow(
        horizontalArrangement = Arrangement.spacedBy(AppSpacing.space3),
        verticalArrangement = Arrangement.spacedBy(AppSpacing.space2),
    ) {
        AppChip(
            text = stringResource(Res.string.addons_filter_all, allCount),
            selected = current == AddonFilter.All,
            onClick = { onSelect(AddonFilter.All) },
        )
        AppChip(
            text = stringResource(Res.string.addons_installed_section, installedCount),
            selected = current == AddonFilter.Installed,
            onClick = { onSelect(AddonFilter.Installed) },
        )
        AppChip(
            text = stringResource(Res.string.addons_available_section, availableCount),
            selected = current == AddonFilter.Available,
            onClick = { onSelect(AddonFilter.Available) },
        )
    }
}

/**
 * One row per add-on: logo, name, a one-line tagline, and an Installed badge
 * when it applies. The tagline is capped at one line so every row is the
 * same height (design-system.md § Typography: a scannable row's text never
 * changes the row's height) — the full text is in the detail pane.
 */
@Composable
private fun AddonList(
    scripts: List<FydeScript>,
    enabledScriptIds: Set<String>,
    selectedScriptId: String?,
    rowMinHeight: Dp,
    onSelect: (FydeScript) -> Unit,
    modifier: Modifier = Modifier,
) {
    LazyColumn(modifier = modifier.fillMaxWidth(), contentPadding = PaddingValues(top = AppSpacing.space2, bottom = AppSpacing.space7)) {
        items(scripts, key = { it.id }) { script ->
            AppListItem(
                title = script.name,
                supportingText = script.shortDescription.ifBlank { script.description }.ifBlank { null },
                supportingTextMaxLines = 1,
                leadingContent = { AddonIcon(icon = script.icon, size = 40.dp) },
                trailingContent = {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        if (script.id in enabledScriptIds) {
                            AppBadge(text = stringResource(Res.string.addon_installed_badge), variant = AppBadgeVariant.Success)
                            Spacer(Modifier.width(AppSpacing.space3))
                        }
                        ChevronRightGlyph(modifier = Modifier.size(12.dp))
                    }
                },
                selected = script.id == selectedScriptId,
                onClick = { onSelect(script) },
                modifier = Modifier.heightIn(min = rowMinHeight),
            )
            AppDivider()
        }
    }
}

/**
 * A script's version and last-update date as one Data-role line, for
 * [ScriptListScreen]'s rows — localized via [Res.string.script_version], the
 * same "v3" label [AddonDetailContent]'s Version cell uses.
 */
@Composable
internal fun scriptVersionLine(script: FydeScript): String =
    "${stringResource(Res.string.script_version, script.version)} · ${formatDate(script.lastUpdated)}"

/**
 * [icon]'s image content never fills [AddonIcon]'s full chip — publisher
 * logos arrive in wildly inconsistent shapes/paddings (a flat-filled square,
 * a circular mark on transparent, ...; see each scraper's `logo.png` under
 * `scripts/scripts/` in the `scripts` repo), and filling the chip edge-to-edge
 * let each one's own background show through (or not) at random, so chips
 * looked inconsistently "framed" next to each other. Insetting every logo
 * the same amount makes the chip read as a deliberate, uniform frame instead.
 */
private const val ICON_IMAGE_SCALE = 0.72f
private const val ICON_GLYPH_SCALE = 0.45f

/**
 * A neutral chip showing [icon] (the script's own PNG logo, see
 * [FydeScript.icon]), decoded via [decodeIconBitmap]. Falls back to a drawn
 * [PuzzleGlyph] when [icon] is empty or isn't valid image data. [size]
 * defaults to a compact chip; the add-on list and [ScriptListScreen] pass a
 * row-sized one, [AddonDetailContent] a larger one for its header.
 *
 * Neutral (page background + hairline border), not tinted with `primary`.
 * Found via review: third-party brand marks (red, blue, purple) on a teal
 * wash clashed, and spent the one accent color on decoration rather than
 * emphasis (design-system.md § Direction, "one disciplined accent").
 */
@Composable
internal fun AddonIcon(icon: ByteArray = ByteArray(0), modifier: Modifier = Modifier, size: Dp = 44.dp) {
    val bitmap = remember(icon) { decodeIconBitmap(icon) }
    Box(
        modifier = modifier
            .size(size)
            .clip(AppRadius.md)
            .background(AppColors.background)
            .border(AppDimensions.borderHairline, AppColors.border, AppRadius.md),
        contentAlignment = Alignment.Center,
    ) {
        if (bitmap != null) {
            Image(
                bitmap = bitmap,
                contentDescription = null,
                contentScale = ContentScale.Fit,
                modifier = Modifier.size(size * ICON_IMAGE_SCALE).clip(AppRadius.sm),
            )
        } else {
            PuzzleGlyph(tint = AppColors.textMuted, modifier = Modifier.size(size * ICON_GLYPH_SCALE))
        }
    }
}

/** Same anatomy as [AddonList]'s rows — icon, name line, tagline line — so loading doesn't shift the layout. */
@Composable
private fun AddonSkeletonList(modifier: Modifier = Modifier) {
    Column(modifier = modifier.fillMaxWidth().padding(top = AppSpacing.space2)) {
        repeat(6) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = AppSpacing.space5, vertical = AppSpacing.space4),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                AppSkeleton(modifier = Modifier.size(40.dp), shape = AppRadius.md)
                Spacer(Modifier.width(AppSpacing.space4))
                Column {
                    AppSkeleton(modifier = Modifier.size(width = 140.dp, height = 14.dp))
                    Spacer(Modifier.height(AppSpacing.space2))
                    AppSkeleton(modifier = Modifier.size(width = 200.dp, height = 10.dp))
                }
            }
        }
    }
}
