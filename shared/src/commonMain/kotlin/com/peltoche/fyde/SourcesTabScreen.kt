package com.peltoche.fyde

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Text
import androidx.compose.material3.ripple
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import com.peltoche.fyde.components.action.AppBackButton
import com.peltoche.fyde.components.action.AppButton
import com.peltoche.fyde.components.action.AppButtonVariant
import com.peltoche.fyde.components.action.AppCircleButton
import com.peltoche.fyde.components.action.AppTextButton
import com.peltoche.fyde.components.content.AppCard
import com.peltoche.fyde.components.content.AppDivider
import com.peltoche.fyde.components.content.AppGroup
import com.peltoche.fyde.components.content.AppGroupRow
import com.peltoche.fyde.components.content.AppIconTile
import com.peltoche.fyde.components.content.AppSectionTitle
import com.peltoche.fyde.components.feedback.AppEmptyState
import com.peltoche.fyde.components.feedback.AppErrorNote
import com.peltoche.fyde.components.feedback.AppErrorState
import com.peltoche.fyde.components.feedback.AppListSkeleton
import com.peltoche.fyde.components.feedback.AppLoading
import com.peltoche.fyde.components.feedback.AppNote
import com.peltoche.fyde.components.feedback.AppNoteKind
import com.peltoche.fyde.components.feedback.RunStepper
import com.peltoche.fyde.components.feedback.SourceStatus
import com.peltoche.fyde.components.feedback.SourceStatusKind
import com.peltoche.fyde.components.form.AppSearchField
import com.peltoche.fyde.components.overlay.AppDialog
import com.peltoche.fyde.icons.AlertGlyph
import com.peltoche.fyde.icons.CheckGlyph
import com.peltoche.fyde.icons.ChevronRightGlyph
import com.peltoche.fyde.icons.InfoGlyph
import com.peltoche.fyde.icons.PlusGlyph
import com.peltoche.fyde.icons.PuzzleGlyph
import com.peltoche.fyde.icons.SearchGlyph
import com.peltoche.fyde.icons.StopGlyph
import com.peltoche.fyde.icons.SyncGlyph
import com.peltoche.fyde.layout.AppContentContainer
import com.peltoche.fyde.layout.AppContentWidth
import com.peltoche.fyde.layout.AppWindowSizeClass
import com.peltoche.fyde.layout.LocalAppWindowSizeClass
import com.peltoche.fyde.resources.Res
import com.peltoche.fyde.resources.cancel
import com.peltoche.fyde.resources.catalogue_add
import com.peltoche.fyde.resources.catalogue_added
import com.peltoche.fyde.resources.catalogue_all
import com.peltoche.fyde.resources.catalogue_limit
import com.peltoche.fyde.resources.catalogue_own
import com.peltoche.fyde.resources.catalogue_search_placeholder
import com.peltoche.fyde.resources.connect_failed_title
import com.peltoche.fyde.resources.connect_source_encrypted
import com.peltoche.fyde.resources.connecting_message
import com.peltoche.fyde.resources.document_detail_value_unset
import com.peltoche.fyde.resources.retry
import com.peltoche.fyde.resources.script_parameter_hidden
import com.peltoche.fyde.resources.script_parameter_no
import com.peltoche.fyde.resources.script_parameter_yes
import com.peltoche.fyde.resources.script_run_button
import com.peltoche.fyde.resources.script_save_button
import com.peltoche.fyde.resources.source_credentials
import com.peltoche.fyde.resources.source_edit
import com.peltoche.fyde.resources.source_fetch_background
import com.peltoche.fyde.resources.source_fetch_continue
import com.peltoche.fyde.resources.source_fetch_finished
import com.peltoche.fyde.resources.source_fetch_title_failed
import com.peltoche.fyde.resources.source_fetching_back
import com.peltoche.fyde.resources.source_fetching_background
import com.peltoche.fyde.resources.source_fetching_stop
import com.peltoche.fyde.resources.source_fetching_title
import com.peltoche.fyde.resources.source_last_run
import com.peltoche.fyde.resources.source_remove
import com.peltoche.fyde.resources.source_remove_confirm
import com.peltoche.fyde.resources.source_remove_text
import com.peltoche.fyde.resources.source_remove_title
import com.peltoche.fyde.resources.source_rerun
import com.peltoche.fyde.resources.source_settings
import com.peltoche.fyde.resources.source_sites
import com.peltoche.fyde.resources.source_status_failed
import com.peltoche.fyde.resources.source_status_queued
import com.peltoche.fyde.resources.source_status_stopped
import com.peltoche.fyde.resources.source_fetching_stopped_title
import com.peltoche.fyde.resources.source_fetching_stopped_note
import com.peltoche.fyde.resources.script_stop_button
import com.peltoche.fyde.resources.source_status_running
import com.peltoche.fyde.resources.source_status_running_count
import com.peltoche.fyde.resources.source_status_up_to_date
import com.peltoche.fyde.resources.source_status_waiting
import com.peltoche.fyde.resources.source_update_rerun
import com.peltoche.fyde.resources.sources_empty
import com.peltoche.fyde.resources.sources_no_match
import com.peltoche.fyde.resources.sources_search_clear
import com.peltoche.fyde.resources.sources_tab_add
import com.peltoche.fyde.resources.sources_tab_column_source
import com.peltoche.fyde.resources.sources_tab_column_status
import com.peltoche.fyde.resources.sources_tab_connected
import com.peltoche.fyde.resources.sources_tab_empty_description
import com.peltoche.fyde.resources.sources_tab_empty_title
import com.peltoche.fyde.resources.sources_tab_needs_you
import com.peltoche.fyde.resources.sources_tab_pick_hint
import com.peltoche.fyde.resources.sources_tab_plan
import com.peltoche.fyde.resources.sources_tab_plan_full
import com.peltoche.fyde.resources.sources_tab_see_plan
import com.peltoche.fyde.resources.sources_tab_sort
import com.peltoche.fyde.resources.sources_tab_title
import com.peltoche.fyde.theme.AppColors
import com.peltoche.fyde.theme.AppDimensions
import com.peltoche.fyde.theme.AppIconSize
import com.peltoche.fyde.theme.AppRadius
import com.peltoche.fyde.theme.AppShapes
import com.peltoche.fyde.theme.AppSpacing
import com.peltoche.fyde.theme.AppTypography
import com.peltoche.fyde.theme.appFocusRing
import org.jetbrains.compose.resources.stringResource

/**
 * What the Sources tab shows, mapped by `App.kt` from `fyde-sdk`'s script
 * calls; no SDK call happens below it.
 */
sealed interface SourcesTabUiState {
    data object Connecting : SourcesTabUiState

    data class ConnectFailed(val message: String, val onRetry: () -> Unit) : SourcesTabUiState

    /**
     * [scripts] is every scraper on offer (the public ones and the user's
     * own), `null` while it's fetched; [installedIds] those the user has
     * connected, each installed with its [installedParameters]. A source in
     * [pendingIds] has an install or removal in flight; [errors] says, by
     * source, why its last install failed.
     */
    data class Loaded(
        val scripts: List<FydeScript>?,
        val installedIds: Set<String> = emptySet(),
        val installedParameters: Map<String, Map<String, FydeScriptParameterValue>> = emptyMap(),
        val quota: SourceQuota? = null,
        val pendingIds: Set<String> = emptySet(),
        val errors: Map<String, String> = emptyMap(),
    ) : SourcesTabUiState {
        val installed: List<FydeScript> = scripts.orEmpty().filter { it.id in installedIds }.sortedBy { it.name.lowercase() }

        /** Whether the plan leaves no room for one more source. */
        val atLimit: Boolean get() = quota != null && !quota.allowsAnother(0)
    }
}

/**
 * Which source page is open over the list. At Compact and Medium each is a
 * page of its own; at Expanded [Connect] fills the main area and the others
 * open in the detail panel, [Fetching] reading there as [Detail] (its
 * "Last run" stepper shows the same run).
 */
sealed interface SourceFocus {
    val scriptId: String

    /** S5, or S6 while its last run failed. */
    data class Detail(override val scriptId: String) : SourceFocus

    /** S3. */
    data class Connect(override val scriptId: String) : SourceFocus

    /** S4. */
    data class Fetching(override val scriptId: String) : SourceFocus

    /** S5's Identifiants, edited. */
    data class Credentials(override val scriptId: String) : SourceFocus
}

/**
 * The Sources tab: S1–S7 as pages at Compact and Medium, D4 at Expanded
 * (a sortable table beside a detail panel). Statuses come from this app's
 * own runs ([statuses], by source); `fyde-sdk` keeps no run history,
 * schedule, provenance or category yet, so the mockups' next run, counts,
 * "Automatique", latest documents, category chips and pausing are left out.
 */
@Composable
fun SourcesTabScreen(
    state: SourcesTabUiState,
    statuses: Map<String, SourceFetchStatus>,
    runningIds: Set<String>,
    stoppingIds: Set<String>,
    focus: SourceFocus?,
    onFocusChange: (SourceFocus?) -> Unit,
    catalogueOpen: Boolean,
    onCatalogueOpenChange: (Boolean) -> Unit,
    query: String,
    onQueryChange: (String) -> Unit,
    onConnect: (FydeScript, Map<String, FydeScriptParameterValue>) -> Unit,
    onUpdate: (FydeScript, Map<String, FydeScriptParameterValue>, rerun: Boolean) -> Unit,
    onRun: (FydeScript) -> Unit,
    onStop: (FydeScript) -> Unit,
    onRemove: (FydeScript) -> Unit,
    onOpenPlan: () -> Unit,
    navigation: TopLevelNavigation,
    modifier: Modifier = Modifier,
) {
    var removing by remember { mutableStateOf<FydeScript?>(null) }
    TopLevelScaffold(
        // Re-selecting the tab from one of its pages returns to the list.
        navigation = navigation.copy(
            onOpenScripts = {
                onFocusChange(null)
                onCatalogueOpenChange(false)
            },
        ),
        selection = SidebarSelection.Sources,
        modifier = modifier,
    ) { contentPadding ->
        val windowSizeClass = LocalAppWindowSizeClass.current
        val pageModifier = Modifier.fillMaxSize().padding(contentPadding).background(AppColors.background)
        when (state) {
            SourcesTabUiState.Connecting -> AppLoading(modifier = pageModifier, message = stringResource(Res.string.connecting_message))
            is SourcesTabUiState.ConnectFailed -> AppErrorState(
                modifier = pageModifier,
                icon = { AlertGlyph(modifier = Modifier.size(AppIconSize.lg)) },
                title = stringResource(Res.string.connect_failed_title),
                description = state.message,
                actionLabel = stringResource(Res.string.retry),
                onAction = state.onRetry,
            )
            is SourcesTabUiState.Loaded -> {
                val tab = SourcesTab(
                    state = state,
                    statuses = statuses,
                    runningIds = runningIds,
                    stoppingIds = stoppingIds,
                    focus = focus,
                    onFocusChange = onFocusChange,
                    onCatalogueOpenChange = onCatalogueOpenChange,
                    query = query,
                    onQueryChange = onQueryChange,
                    onConnect = onConnect,
                    onUpdate = onUpdate,
                    onRun = onRun,
                    onStop = onStop,
                    onRemove = { removing = it },
                    onOpenPlan = onOpenPlan,
                )
                if (windowSizeClass == AppWindowSizeClass.Expanded) {
                    WideSourcesTab(tab, catalogueOpen, pageModifier)
                } else {
                    NarrowSourcesTab(tab, catalogueOpen, pageModifier)
                }
            }
        }
    }

    removing?.let { script ->
        AppDialog(
            onDismissRequest = { removing = null },
            title = stringResource(Res.string.source_remove_title, script.name),
            text = stringResource(Res.string.source_remove_text, script.name),
            confirmLabel = stringResource(Res.string.source_remove_confirm),
            onConfirm = {
                removing = null
                onRemove(script)
            },
            dismissLabel = stringResource(Res.string.cancel),
            onDismiss = { removing = null },
            destructive = true,
        )
    }
}

/** Everything the tab's pages read and call, passed down as one. */
private class SourcesTab(
    val state: SourcesTabUiState.Loaded,
    val statuses: Map<String, SourceFetchStatus>,
    val runningIds: Set<String>,
    val stoppingIds: Set<String>,
    val focus: SourceFocus?,
    val onFocusChange: (SourceFocus?) -> Unit,
    val onCatalogueOpenChange: (Boolean) -> Unit,
    val query: String,
    val onQueryChange: (String) -> Unit,
    val onConnect: (FydeScript, Map<String, FydeScriptParameterValue>) -> Unit,
    val onUpdate: (FydeScript, Map<String, FydeScriptParameterValue>, Boolean) -> Unit,
    val onRun: (FydeScript) -> Unit,
    val onStop: (FydeScript) -> Unit,
    val onRemove: (FydeScript) -> Unit,
    val onOpenPlan: () -> Unit,
) {
    fun script(id: String): FydeScript? = state.scripts?.find { it.id == id }

    fun isInstalled(id: String): Boolean = id in state.installedIds

    fun isPending(id: String): Boolean = id in state.pendingIds

    /** Whether [id] has a run in progress; other sources' runs don't matter (runs are parallel). */
    fun isRunning(id: String): Boolean = id in runningIds

    /** Whether [id]'s run was asked to stop and hasn't ended yet. */
    fun isStopping(id: String): Boolean = id in stoppingIds

    fun installedParameters(id: String): Map<String, FydeScriptParameterValue> = state.installedParameters[id].orEmpty()
}

/** Compact and Medium: one page at a time, S1 at the bottom of the stack. */
@Composable
private fun NarrowSourcesTab(tab: SourcesTab, catalogueOpen: Boolean, modifier: Modifier) {
    val focus = tab.focus
    val script = focus?.let { tab.script(it.scriptId) }
    AppContentContainer(modifier = modifier, width = AppContentWidth.Detail) {
        when {
            focus is SourceFocus.Connect && script != null -> key(script.id) {
                ConnectPage(tab, script, onBack = { tab.onFocusChange(null) })
            }
            focus is SourceFocus.Fetching && script != null -> key(script.id) { FetchingPage(tab, script) }
            focus is SourceFocus.Credentials && script != null && tab.isInstalled(script.id) -> key(script.id) {
                CredentialsPage(tab, script, onBack = { tab.onFocusChange(SourceFocus.Detail(script.id)) })
            }
            focus != null && script != null && tab.isInstalled(script.id) -> key(script.id) { DetailPage(tab, script) }
            catalogueOpen -> CataloguePage(tab, onBack = { tab.onCatalogueOpenChange(false) })
            else -> SourceListPage(tab)
        }
    }
}

/**
 * D4: the table (or the catalogue, or a source's connect form, in its place)
 * beside a detail panel flush to the window's right edge, showing the
 * selected source — the first one until another is picked.
 */
@Composable
private fun WideSourcesTab(tab: SourcesTab, catalogueOpen: Boolean, modifier: Modifier) {
    val focus = tab.focus
    val connecting = (focus as? SourceFocus.Connect)?.let { tab.script(it.scriptId) }
    val selected = focus?.takeIf { it !is SourceFocus.Connect && tab.isInstalled(it.scriptId) }?.let { tab.script(it.scriptId) }
        ?: tab.state.installed.firstOrNull()
    Row(modifier = modifier) {
        Box(modifier = Modifier.weight(1f).fillMaxHeight()) {
            when {
                connecting != null -> AppContentContainer(width = AppContentWidth.Detail) {
                    key(connecting.id) { ConnectPage(tab, connecting, onBack = { tab.onFocusChange(null) }) }
                }
                catalogueOpen -> AppContentContainer(width = AppContentWidth.Detail) {
                    CataloguePage(tab, onBack = { tab.onCatalogueOpenChange(false) })
                }
                else -> AppContentContainer(width = AppContentWidth.List) { SourceTablePage(tab, selected?.id) }
            }
        }
        AppDivider(vertical = true)
        Box(
            modifier = Modifier
                .width(AppDimensions.sourceDetailPanelWidth)
                .fillMaxHeight()
                .background(AppColors.surface),
        ) {
            if (selected == null) {
                Text(
                    text = stringResource(Res.string.sources_tab_pick_hint),
                    style = AppTypography.body,
                    color = AppColors.textMuted,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.align(Alignment.Center).padding(AppSpacing.space7),
                )
            } else {
                key(selected.id) {
                    Column(modifier = Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(AppSpacing.space6)) {
                        if (focus is SourceFocus.Credentials && focus.scriptId == selected.id) {
                            AppBackButton(onClick = { tab.onFocusChange(SourceFocus.Detail(selected.id)) })
                            Spacer(Modifier.height(AppSpacing.space4))
                            CredentialsBody(tab, selected, autofocus = true)
                        } else {
                            DetailBody(tab, selected, wide = true)
                        }
                    }
                }
            }
        }
    }
}

// S1 ------------------------------------------------------------------------------------------

/** S1: the sources that need the user first, then the others, and the plan's count. */
@Composable
private fun SourceListPage(tab: SourcesTab) {
    val installed = tab.state.installed
    Column(modifier = Modifier.fillMaxSize().verticalScroll(rememberScrollState())) {
        Spacer(Modifier.height(AppSpacing.space5))
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(
                text = stringResource(Res.string.sources_tab_title),
                style = AppTypography.display,
                color = AppColors.text,
                modifier = Modifier.weight(1f).semantics { heading() },
            )
            AppCircleButton(
                onClick = { tab.onCatalogueOpenChange(true) },
                contentDescription = stringResource(Res.string.sources_tab_add),
            ) {
                PlusGlyph(modifier = Modifier.size(AppIconSize.md), tint = AppColors.text)
            }
        }
        when {
            tab.state.scripts == null -> {
                Spacer(Modifier.height(AppSpacing.space7))
                SkeletonRows()
            }
            installed.isEmpty() -> EmptySources(tab)
            else -> {
                val needsYou = installed.filter { tab.statuses[it.id].needsUser() }
                val others = installed - needsYou.toSet()
                if (needsYou.isNotEmpty()) {
                    AppSectionTitle(stringResource(Res.string.sources_tab_needs_you))
                    SourceRows(tab, needsYou)
                }
                if (others.isNotEmpty()) {
                    AppSectionTitle(stringResource(Res.string.sources_tab_connected))
                    SourceRows(tab, others)
                }
                PlanLine(tab, centered = true, modifier = Modifier.fillMaxWidth().padding(top = AppSpacing.space5))
            }
        }
        Spacer(Modifier.height(AppSpacing.space7))
    }
}

@Composable
private fun SourceRows(tab: SourcesTab, scripts: List<FydeScript>) {
    AppGroup {
        scripts.forEachIndexed { index, script ->
            if (index > 0) AppDivider()
            SourceRow(
                script = script,
                status = tab.statuses[script.id],
                onClick = { tab.onFocusChange(SourceFocus.Detail(script.id)) },
            )
        }
    }
}

/** One S1 row: tile, name, status (or what it fetches until it has one), chevron. */
@Composable
private fun SourceRow(script: FydeScript, status: SourceFetchStatus?, onClick: () -> Unit) {
    AppGroupRow(onClick = onClick, minHeight = AppDimensions.rowHeightComfortable) {
        ScriptIcon(icon = script.icon)
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = script.name,
                style = AppTypography.bodyEmphasis,
                color = AppColors.text,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
            if (status != null) {
                SourceStatusLine(status)
            } else {
                Summary(script)
            }
        }
        ChevronRightGlyph(modifier = Modifier.size(AppIconSize.sm))
    }
}

@Composable
private fun EmptySources(tab: SourcesTab) {
    AppEmptyState(
        modifier = Modifier.fillMaxWidth().padding(vertical = AppSpacing.space9),
        icon = { PlusGlyph(modifier = Modifier.size(AppIconSize.lg), tint = AppColors.textMuted) },
        title = stringResource(Res.string.sources_tab_empty_title),
        description = stringResource(Res.string.sources_tab_empty_description),
        actionLabel = stringResource(Res.string.sources_tab_add),
        onAction = { tab.onCatalogueOpenChange(true) },
    )
}

/** "6 of 8 sources on the Basic plan", and the way to the plan once it's full. Nothing without a limit. */
@Composable
private fun PlanLine(tab: SourcesTab, centered: Boolean, modifier: Modifier = Modifier) {
    val quota = tab.state.quota ?: return
    val plan = planName(quota.planId)
    Column(modifier = modifier, horizontalAlignment = if (centered) Alignment.CenterHorizontally else Alignment.Start) {
        Text(
            text = stringResource(
                if (tab.state.atLimit) Res.string.sources_tab_plan_full else Res.string.sources_tab_plan,
                quota.active,
                quota.max,
                plan,
            ),
            style = AppTypography.caption,
            color = AppColors.textMuted,
            textAlign = if (centered) TextAlign.Center else TextAlign.Start,
        )
        if (tab.state.atLimit) {
            AppTextButton(text = stringResource(Res.string.sources_tab_see_plan), onClick = tab.onOpenPlan)
        }
    }
}

// D4's table ----------------------------------------------------------------------------------

private enum class SourceColumn { Name, Status }

private data class SourceSort(val column: SourceColumn, val ascending: Boolean)

/** D4's main area: the title, the plan's count, Ajouter une source, and the sortable table. */
@Composable
private fun SourceTablePage(tab: SourcesTab, selectedId: String?) {
    var sort by remember { mutableStateOf(SourceSort(SourceColumn.Name, ascending = true)) }
    val rows = remember(tab.state.installed, tab.statuses, sort) { sortedSources(tab.state.installed, tab.statuses, sort) }
    Column(modifier = Modifier.fillMaxSize().verticalScroll(rememberScrollState())) {
        Spacer(Modifier.height(AppSpacing.space7))
        Row(verticalAlignment = Alignment.CenterVertically) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = stringResource(Res.string.sources_tab_title),
                    style = AppTypography.display,
                    color = AppColors.text,
                    modifier = Modifier.semantics { heading() },
                )
                PlanLine(tab, centered = false)
            }
            AppButton(
                text = stringResource(Res.string.sources_tab_add),
                onClick = { tab.onCatalogueOpenChange(true) },
                leadingIcon = { PlusGlyph(modifier = Modifier.size(AppIconSize.sm), tint = AppColors.onPrimary) },
            )
        }
        Spacer(Modifier.height(AppSpacing.space6))
        when {
            tab.state.scripts == null -> SkeletonRows()
            rows.isEmpty() -> EmptySources(tab)
            else -> {
                Row(modifier = Modifier.fillMaxWidth()) {
                    SortHeader(stringResource(Res.string.sources_tab_column_source), SourceColumn.Name, sort, { sort = it }, Modifier.weight(1f))
                    SortHeader(stringResource(Res.string.sources_tab_column_status), SourceColumn.Status, sort, { sort = it }, Modifier.weight(1f))
                }
                AppDivider()
                rows.forEach { script ->
                    SourceTableRow(
                        script = script,
                        status = tab.statuses[script.id],
                        selected = script.id == selectedId,
                        onClick = { tab.onFocusChange(SourceFocus.Detail(script.id)) },
                    )
                    AppDivider()
                }
            }
        }
        Spacer(Modifier.height(AppSpacing.space7))
    }
}

/** A column's header: clicking sorts by it, clicking again reverses; a chevron marks the active one's direction. */
@Composable
private fun SortHeader(
    label: String,
    column: SourceColumn,
    sort: SourceSort,
    onSort: (SourceSort) -> Unit,
    modifier: Modifier,
) {
    val active = sort.column == column
    val sortLabel = stringResource(Res.string.sources_tab_sort, label)
    val interactionSource = remember { MutableInteractionSource() }
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(AppSpacing.space2),
        modifier = modifier
            .clickable(interactionSource = interactionSource, indication = ripple(), role = Role.Button) {
                onSort(if (active) sort.copy(ascending = !sort.ascending) else SourceSort(column, ascending = true))
            }
            .appFocusRing(interactionSource, shape = AppRadius.sm)
            .semantics { contentDescription = sortLabel }
            .padding(horizontal = AppSpacing.space4, vertical = AppSpacing.space4),
    ) {
        Text(
            text = label.uppercase(),
            style = AppTypography.caption.copy(fontWeight = FontWeight.SemiBold),
            color = AppColors.textMuted,
        )
        if (active) {
            ChevronRightGlyph(
                modifier = Modifier.size(AppIconSize.xs).rotate(if (sort.ascending) -90f else 90f),
            )
        }
    }
}

/** One D4 row: tile and name, then its status — "—" until this app has run it. */
@Composable
private fun SourceTableRow(script: FydeScript, status: SourceFetchStatus?, selected: Boolean, onClick: () -> Unit) {
    AppGroupRow(
        onClick = onClick,
        minHeight = AppDimensions.rowHeightStandard,
        background = if (selected) AppColors.selectionSurface else Color.Transparent,
        horizontalPadding = AppSpacing.space4,
    ) {
        Row(modifier = Modifier.weight(1f), verticalAlignment = Alignment.CenterVertically) {
            ScriptIcon(icon = script.icon, size = AppDimensions.iconTileSmall)
            Spacer(Modifier.width(AppSpacing.space4))
            Text(
                text = script.name,
                style = AppTypography.body.copy(fontWeight = FontWeight.SemiBold),
                color = AppColors.text,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
        }
        Box(modifier = Modifier.weight(1f).padding(start = AppSpacing.space4)) {
            if (status != null) {
                SourceStatusLine(status)
            } else {
                Text(text = "—", style = AppTypography.caption, color = AppColors.textMuted)
            }
        }
    }
}

/** [sources] in [sort]'s order; by status, those needing the user first and those never run last, then by name. */
private fun sortedSources(
    sources: List<FydeScript>,
    statuses: Map<String, SourceFetchStatus>,
    sort: SourceSort,
): List<FydeScript> {
    val byName = compareBy<FydeScript> { it.name.lowercase() }
    val comparator = when (sort.column) {
        SourceColumn.Name -> byName
        SourceColumn.Status -> compareBy<FydeScript> { statusRank(statuses[it.id]) }.then(byName)
    }
    return sources.sortedWith(if (sort.ascending) comparator else comparator.reversed())
}

private fun statusRank(status: SourceFetchStatus?): Int = when (status) {
    is SourceFetchStatus.Failed -> 0
    is SourceFetchStatus.Running -> if (status.step.isWaitingForUser()) 1 else 2
    SourceFetchStatus.Queued -> 3
    is SourceFetchStatus.Stopped -> 4
    is SourceFetchStatus.Succeeded -> 5
    null -> 6
}

// S2 ------------------------------------------------------------------------------------------

/** S2: every scraper on offer, searchable; the user's own scripts in a last group. */
@Composable
private fun CataloguePage(tab: SourcesTab, onBack: () -> Unit) {
    val scripts = tab.state.scripts
    Column(modifier = Modifier.fillMaxSize().verticalScroll(rememberScrollState())) {
        PageBar(title = stringResource(Res.string.sources_tab_add), onBack = onBack)
        Spacer(Modifier.height(AppSpacing.space3))
        AppSearchField(
            value = tab.query,
            onValueChange = tab.onQueryChange,
            placeholder = stringResource(Res.string.catalogue_search_placeholder),
            onClear = { tab.onQueryChange("") },
            clearContentDescription = stringResource(Res.string.sources_search_clear),
            leadingIcon = { SearchGlyph(modifier = Modifier.size(AppIconSize.sm)) },
        )
        val quota = tab.state.quota
        if (tab.state.atLimit && quota != null) {
            Spacer(Modifier.height(AppSpacing.space5))
            AppNote(
                message = stringResource(Res.string.catalogue_limit, planName(quota.planId)),
                kind = AppNoteKind.Warning,
                icon = { tint -> AlertGlyph(modifier = Modifier.size(AppIconSize.sm), tint = tint) },
            )
            AppTextButton(text = stringResource(Res.string.sources_tab_see_plan), onClick = tab.onOpenPlan)
        }
        when {
            scripts == null -> {
                Spacer(Modifier.height(AppSpacing.space7))
                SkeletonRows()
            }
            scripts.isEmpty() -> CatalogueMessage(stringResource(Res.string.sources_empty))
            else -> {
                val matches = filterScripts(scripts, tab.query).sortedBy { it.name.lowercase() }
                val public = matches.filter { it.isPublic }
                val own = matches.filter { !it.isPublic }
                if (matches.isEmpty()) CatalogueMessage(stringResource(Res.string.sources_no_match, tab.query.trim()))
                if (public.isNotEmpty()) {
                    AppSectionTitle(stringResource(Res.string.catalogue_all))
                    CatalogueRows(tab, public)
                }
                if (own.isNotEmpty()) {
                    AppSectionTitle(stringResource(Res.string.catalogue_own))
                    CatalogueRows(tab, own)
                }
            }
        }
        Spacer(Modifier.height(AppSpacing.space7))
    }
}

@Composable
private fun CatalogueMessage(text: String) {
    Text(
        text = text,
        style = AppTypography.body,
        color = AppColors.textMuted,
        textAlign = TextAlign.Center,
        modifier = Modifier.fillMaxWidth().padding(vertical = AppSpacing.space8),
    )
}

@Composable
private fun CatalogueRows(tab: SourcesTab, scripts: List<FydeScript>) {
    AppGroup {
        scripts.forEachIndexed { index, script ->
            if (index > 0) AppDivider()
            val added = tab.isInstalled(script.id)
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(AppSpacing.space4),
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(min = AppDimensions.rowHeightComfortable)
                    .padding(horizontal = AppSpacing.space5, vertical = AppSpacing.space3),
            ) {
                ScriptIcon(icon = script.icon)
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = script.name,
                        style = AppTypography.bodyEmphasis,
                        color = AppColors.text,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                    if (added) {
                        Text(text = stringResource(Res.string.catalogue_added), style = AppTypography.caption, color = AppColors.textMuted)
                    } else {
                        Summary(script, maxLines = 2)
                    }
                }
                if (added) {
                    CheckGlyph(modifier = Modifier.size(AppIconSize.md))
                } else {
                    AppButton(
                        text = stringResource(Res.string.catalogue_add),
                        onClick = { tab.onFocusChange(SourceFocus.Connect(script.id)) },
                        variant = AppButtonVariant.Outlined,
                        enabled = !tab.state.atLimit,
                    )
                }
            }
        }
    }
}

// S3 ------------------------------------------------------------------------------------------

/** S3: what the source fetches, the sites it may reach, its fields; the connect button stays pinned at the bottom. */
@Composable
private fun ConnectPage(tab: SourcesTab, script: FydeScript, onBack: () -> Unit) {
    val values = remember { mutableStateMapOf<String, String>() }
    var showRequiredErrors by remember { mutableStateOf(false) }
    val form = ConnectSourceForm(
        script = script,
        values = values,
        showRequiredErrors = showRequiredErrors,
        errorMessage = tab.state.errors[script.id],
        isSubmitting = tab.isPending(script.id),
        onSubmit = {
            val entered = validatedParameterValues(script.parameters, values)
            if (entered != null) tab.onConnect(script, entered) else showRequiredErrors = true
        },
    )
    Column(modifier = Modifier.fillMaxSize()) {
        PageBar(title = null, onBack = onBack, enabled = !form.isSubmitting)
        Column(modifier = Modifier.weight(1f).fillMaxWidth().verticalScroll(rememberScrollState())) {
            Spacer(Modifier.height(AppSpacing.space4))
            ConnectSourceForm(form, encryptedNote = Res.string.connect_source_encrypted)
            Spacer(Modifier.height(AppSpacing.space6))
        }
        Box(modifier = Modifier.fillMaxWidth().padding(top = AppSpacing.space4, bottom = AppSpacing.space5)) {
            ConnectButton(form)
        }
    }
}

// S4 ------------------------------------------------------------------------------------------

/** S4: the run as a stepper; it carries on in the background whatever the user does next. */
@Composable
private fun FetchingPage(tab: SourcesTab, script: FydeScript) {
    val status = tab.statuses[script.id] ?: SourceFetchStatus.Queued
    val over = status is SourceFetchStatus.Succeeded || status is SourceFetchStatus.Failed || status is SourceFetchStatus.Stopped
    val leave = { tab.onFocusChange(null) }
    Column(modifier = Modifier.fillMaxSize()) {
        PageBar(title = script.name, onBack = leave)
        Column(modifier = Modifier.weight(1f).fillMaxWidth().verticalScroll(rememberScrollState())) {
            Spacer(Modifier.height(AppSpacing.space3))
            Text(
                text = when (status) {
                    is SourceFetchStatus.Succeeded -> stringResource(Res.string.source_fetch_finished)
                    is SourceFetchStatus.Failed -> stringResource(Res.string.source_fetch_title_failed, script.name)
                    is SourceFetchStatus.Stopped -> stringResource(Res.string.source_fetching_stopped_title)
                    else -> stringResource(Res.string.source_fetching_title)
                },
                style = AppTypography.display,
                color = AppColors.text,
                modifier = Modifier.semantics { heading() },
            )
            Spacer(Modifier.height(AppSpacing.space6))
            AppCard(modifier = Modifier.fillMaxWidth(), outlined = true) {
                RunStepper(steps = sourceFetchSteps(status))
            }
            Spacer(Modifier.height(AppSpacing.space5))
            when {
                status is SourceFetchStatus.Failed -> AppErrorNote(status.message)
                // S4b: what it got stays, and Relancer picks up from there.
                status is SourceFetchStatus.Stopped -> AppNote(
                    message = stringResource(Res.string.source_fetching_stopped_note),
                    kind = AppNoteKind.Info,
                    icon = { tint -> InfoGlyph(modifier = Modifier.size(AppIconSize.md), tint = tint) },
                )
                !over -> Text(
                    text = stringResource(Res.string.source_fetch_background, script.name),
                    style = AppTypography.caption,
                    color = AppColors.textMuted,
                )
            }
            Spacer(Modifier.height(AppSpacing.space6))
        }
        Column(
            verticalArrangement = Arrangement.spacedBy(AppSpacing.space2),
            modifier = Modifier.fillMaxWidth().padding(top = AppSpacing.space4, bottom = AppSpacing.space5),
        ) {
            AppButton(
                text = stringResource(
                    when {
                        status is SourceFetchStatus.Stopped -> Res.string.source_fetching_back
                        over -> Res.string.source_fetch_continue
                        else -> Res.string.source_fetching_background
                    },
                ),
                onClick = leave,
                modifier = Modifier.fillMaxWidth(),
            )
            when {
                // S4: never confirmed, never red; busy until the run has really ended.
                !over -> AppTextButton(
                    text = stringResource(Res.string.source_fetching_stop),
                    onClick = { tab.onStop(script) },
                    loading = tab.isStopping(script.id),
                    enabled = !tab.isStopping(script.id),
                    leadingIcon = { StopGlyph(modifier = Modifier.size(AppIconSize.sm), tint = AppColors.primary) },
                    modifier = Modifier.fillMaxWidth(),
                )
                status is SourceFetchStatus.Stopped -> AppTextButton(
                    text = stringResource(Res.string.source_rerun),
                    onClick = { tab.onRun(script) },
                    enabled = !tab.isRunning(script.id),
                    leadingIcon = { SyncGlyph(modifier = Modifier.size(AppIconSize.sm), tint = AppColors.primary) },
                    modifier = Modifier.fillMaxWidth(),
                )
            }
        }
    }
}

// S5, S6 --------------------------------------------------------------------------------------

@Composable
private fun DetailPage(tab: SourcesTab, script: FydeScript) {
    val failed = tab.statuses[script.id] is SourceFetchStatus.Failed
    Column(modifier = Modifier.fillMaxSize().verticalScroll(rememberScrollState())) {
        PageBar(
            title = null,
            onBack = { tab.onFocusChange(null) },
            trailing = if (failed) null else { { RunButton(tab, script) } },
        )
        Spacer(Modifier.height(AppSpacing.space3))
        DetailBody(tab, script, wide = false)
        Spacer(Modifier.height(AppSpacing.space7))
    }
}

/**
 * S5 (or S6 once its last run failed): the header, the last run of this app
 * session, Réglages, Retirer la source. In D4's panel ([wide]) Lancer sits
 * beside the header and Identifiants opens with Modifier rather than a chevron.
 */
@Composable
private fun ColumnScope.DetailBody(tab: SourcesTab, script: FydeScript, wide: Boolean) {
    val status = tab.statuses[script.id]
    Row(verticalAlignment = Alignment.CenterVertically) {
        ScriptIcon(icon = script.icon, size = if (wide) AppDimensions.iconTileHeader else AppDimensions.iconTileLarge)
        Spacer(Modifier.width(AppSpacing.space4))
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = script.name,
                style = AppTypography.title,
                color = AppColors.text,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.semantics { heading() },
            )
            when (status) {
                // The note below gives the reason in full.
                is SourceFetchStatus.Failed -> SourceStatus(SourceStatusKind.Failed, stringResource(Res.string.source_status_failed))
                null -> Summary(script)
                else -> SourceStatusLine(status)
            }
        }
        if (wide && status !is SourceFetchStatus.Failed) {
            Spacer(Modifier.width(AppSpacing.space3))
            RunButton(tab, script)
        }
    }

    if (status is SourceFetchStatus.Failed) {
        Spacer(Modifier.height(AppSpacing.space5))
        AppErrorNote(status.message)
        Spacer(Modifier.height(AppSpacing.space5))
        UpdateForm(tab, script, autofocus = !wide)
    } else {
        if (status != null) {
            AppSectionTitle(stringResource(Res.string.source_last_run))
            AppCard(modifier = Modifier.fillMaxWidth(), outlined = true) {
                RunStepper(steps = sourceFetchSteps(status))
            }
        }
        SourceSettings(tab, script, wide)
    }

    Spacer(Modifier.height(AppSpacing.space6))
    AppButton(
        text = stringResource(Res.string.source_remove),
        onClick = { tab.onRemove(script) },
        variant = AppButtonVariant.Outlined,
        destructive = true,
        enabled = !tab.isPending(script.id),
        modifier = Modifier.fillMaxWidth(),
    )
}

/**
 * Lancer, turned into an outlined Arrêter while this source runs (S5b, D4b),
 * busy until the stopped run has really ended; other sources' runs go in
 * parallel.
 */
@Composable
private fun RunButton(tab: SourcesTab, script: FydeScript) {
    if (tab.isRunning(script.id)) {
        val stopping = tab.isStopping(script.id)
        AppButton(
            text = stringResource(Res.string.script_stop_button),
            onClick = { tab.onStop(script) },
            variant = AppButtonVariant.Outlined,
            loading = stopping,
            enabled = !stopping,
            leadingIcon = { StopGlyph(modifier = Modifier.size(AppIconSize.sm), tint = AppColors.text) },
        )
    } else {
        AppButton(
            text = stringResource(Res.string.script_run_button),
            onClick = { tab.onRun(script) },
            variant = AppButtonVariant.Outlined,
            enabled = !tab.isPending(script.id),
            leadingIcon = { SyncGlyph(modifier = Modifier.size(AppIconSize.sm), tint = AppColors.text) },
        )
    }
}

/** Réglages: the credentials it signs in with (first plain value shown) and the sites it may reach. */
@Composable
private fun SourceSettings(tab: SourcesTab, script: FydeScript, wide: Boolean) {
    val hasCredentials = script.parameters.isNotEmpty()
    if (!hasCredentials && script.allowedDomains.isEmpty()) return
    AppSectionTitle(stringResource(Res.string.source_settings))
    AppGroup {
        if (hasCredentials) {
            val openCredentials = { tab.onFocusChange(SourceFocus.Credentials(script.id)) }
            val label = stringResource(Res.string.source_credentials)
            val value = credentialsSummary(script, tab.installedParameters(script.id))
            if (wide) {
                SettingRow(label = label, value = value) {
                    AppTextButton(text = stringResource(Res.string.source_edit), onClick = openCredentials, enabled = !tab.isPending(script.id))
                }
            } else {
                AppGroupRow(onClick = openCredentials, minHeight = AppDimensions.rowHeightStandard, enabled = !tab.isPending(script.id)) {
                    SettingText(label = label, value = value, modifier = Modifier.weight(1f))
                    ChevronRightGlyph(modifier = Modifier.size(AppIconSize.sm))
                }
            }
        }
        if (script.allowedDomains.isNotEmpty()) {
            if (hasCredentials) AppDivider()
            SettingRow(label = stringResource(Res.string.source_sites), value = script.allowedDomains.joinToString(", "))
        }
    }
}

@Composable
private fun SettingRow(label: String, value: String, trailing: (@Composable () -> Unit)? = null) {
    AppGroupRow(horizontalArrangement = Arrangement.Start) {
        SettingText(label = label, value = value, modifier = Modifier.weight(1f))
        trailing?.invoke()
    }
}

@Composable
private fun SettingText(label: String, value: String, modifier: Modifier) {
    Column(modifier = modifier) {
        Text(text = label, style = AppTypography.body, color = AppColors.text)
        Text(text = value, style = AppTypography.caption, color = AppColors.textMuted, maxLines = 2, overflow = TextOverflow.Ellipsis)
    }
}

/** The first plain credential as entered (an email, a number), else that the secret ones are set. */
@Composable
private fun credentialsSummary(script: FydeScript, installed: Map<String, FydeScriptParameterValue>): String {
    val labels = ParameterValueLabels(
        hidden = stringResource(Res.string.script_parameter_hidden),
        yes = stringResource(Res.string.script_parameter_yes),
        no = stringResource(Res.string.script_parameter_no),
        unset = stringResource(Res.string.document_detail_value_unset),
    )
    val ordered = orderedParameters(script.parameters)
    val (key, parameter) = ordered.firstOrNull { !it.value.secret } ?: ordered.first()
    return displayedParameterValue(parameter, installed[key], labels)
}

/** S6: the fields again, a blank secret keeping the installed one, then Mettre à jour et relancer. */
@Composable
private fun UpdateForm(tab: SourcesTab, script: FydeScript, autofocus: Boolean) {
    if (script.parameters.isEmpty()) {
        AppButton(
            text = stringResource(Res.string.source_rerun),
            onClick = { tab.onRun(script) },
            enabled = !tab.isRunning(script.id),
            modifier = Modifier.fillMaxWidth(),
        )
        return
    }
    CredentialsFields(
        tab = tab,
        script = script,
        submitLabel = stringResource(Res.string.source_update_rerun),
        rerun = true,
        autofocus = autofocus,
    )
}

// Credentials ---------------------------------------------------------------------------------

@Composable
private fun CredentialsPage(tab: SourcesTab, script: FydeScript, onBack: () -> Unit) {
    Column(modifier = Modifier.fillMaxSize().verticalScroll(rememberScrollState())) {
        PageBar(title = script.name, onBack = onBack, enabled = !tab.isPending(script.id))
        Spacer(Modifier.height(AppSpacing.space3))
        CredentialsBody(tab, script, autofocus = true)
        Spacer(Modifier.height(AppSpacing.space7))
    }
}

@Composable
private fun CredentialsBody(tab: SourcesTab, script: FydeScript, autofocus: Boolean) {
    Text(
        text = stringResource(Res.string.source_credentials),
        style = AppTypography.title,
        color = AppColors.text,
        modifier = Modifier.semantics { heading() },
    )
    Spacer(Modifier.height(AppSpacing.space5))
    CredentialsFields(
        tab = tab,
        script = script,
        submitLabel = stringResource(Res.string.script_save_button),
        rerun = false,
        autofocus = autofocus,
    )
}

/**
 * A source's fields prefilled with what it's installed with, secrets left
 * blank (a blank one keeps the installed value), and [submitLabel]
 * reinstalling it — then running it again when [rerun].
 */
@Composable
private fun CredentialsFields(
    tab: SourcesTab,
    script: FydeScript,
    submitLabel: String,
    rerun: Boolean,
    autofocus: Boolean,
) {
    val installed = tab.installedParameters(script.id)
    val keptSecrets = remember(installed) { keptSecretKeys(script.parameters, installed) }
    val values = remember {
        mutableStateMapOf<String, String>().apply {
            putAll(formValues(installed.filterKeys { script.parameters[it]?.secret == false }))
        }
    }
    var showRequiredErrors by remember { mutableStateOf(false) }
    val form = ConnectSourceForm(
        script = script,
        values = values,
        showRequiredErrors = showRequiredErrors,
        errorMessage = tab.state.errors[script.id],
        isSubmitting = tab.isPending(script.id),
        keptSecrets = keptSecrets,
        // Rerunning needs this source's run over: ScraperRuns won't start a
        // second one, so the new credentials would only be saved.
        canSubmit = !(rerun && tab.isRunning(script.id)),
        onSubmit = {
            val entered = validatedCredentials(script.parameters, values, installed)
            if (entered != null) tab.onUpdate(script, entered, rerun) else showRequiredErrors = true
        },
    )
    val focus = remember { FocusRequester() }
    val firstSecret = orderedParameters(script.parameters).firstOrNull { it.value.secret }?.key
    if (autofocus) LaunchedEffect(Unit) { focus.requestFocus() }

    if (form.errorMessage != null) {
        AppErrorNote(form.errorMessage)
        Spacer(Modifier.height(AppSpacing.space5))
    }
    ConnectSourceFields(form, focus, focusedKey = firstSecret)
    Spacer(Modifier.height(AppSpacing.space6))
    ConnectButton(form, text = submitLabel)
}

// Shared pieces -------------------------------------------------------------------------------

/** A page's bar: the back circle, an optional centred [title], an optional [trailing] action. */
@Composable
private fun PageBar(
    title: String?,
    onBack: () -> Unit,
    enabled: Boolean = true,
    trailing: (@Composable () -> Unit)? = null,
) {
    Box(
        modifier = Modifier.fillMaxWidth().heightIn(min = AppDimensions.topBarHeight),
        contentAlignment = Alignment.Center,
    ) {
        AppBackButton(onClick = onBack, enabled = enabled, modifier = Modifier.align(Alignment.CenterStart))
        if (title != null) {
            Text(
                text = title,
                style = AppTypography.bodyEmphasis,
                color = AppColors.text,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.padding(horizontal = AppDimensions.backButton + AppSpacing.space3),
            )
        }
        if (trailing != null) {
            Box(modifier = Modifier.align(Alignment.CenterEnd)) { trailing() }
        }
    }
}

@Composable
private fun Summary(script: FydeScript, maxLines: Int = 1) {
    val summary = script.shortDescription.ifBlank { script.description }
    if (summary.isNotBlank()) {
        Text(text = summary, style = AppTypography.caption, color = AppColors.textMuted, maxLines = maxLines, overflow = TextOverflow.Ellipsis)
    }
}

/** Three placeholder rows while the scripts load. */
@Composable
private fun SkeletonRows() {
    AppListSkeleton(
        rows = 3,
        rowHeight = AppDimensions.rowHeightComfortable,
        tileSize = AppDimensions.iconTile,
        tileShape = AppShapes.iconTile,
        firstLine = Modifier.fillMaxWidth(0.4f).height(AppSpacing.space5),
        secondLine = Modifier.fillMaxWidth(0.6f).height(AppSpacing.space4),
        lineGap = AppSpacing.space2,
    )
}

/** A source's status line, worded for where its run is. */
@Composable
internal fun SourceStatusLine(status: SourceFetchStatus, modifier: Modifier = Modifier) {
    when (status) {
        is SourceFetchStatus.Running -> {
            val update = status.update?.takeIf { it.total > 0 && status.step == FydeScraperStep.DOWNLOADING_DOCUMENTS }
            when {
                status.step.isWaitingForUser() ->
                    SourceStatus(SourceStatusKind.Waiting, stringResource(Res.string.source_status_waiting), modifier)
                update != null -> SourceStatus(
                    SourceStatusKind.Running,
                    stringResource(Res.string.source_status_running_count, update.current, update.total),
                    modifier,
                )
                else -> SourceStatus(SourceStatusKind.Running, stringResource(Res.string.source_status_running), modifier)
            }
        }
        SourceFetchStatus.Queued -> SourceStatus(SourceStatusKind.Queued, stringResource(Res.string.source_status_queued), modifier)
        is SourceFetchStatus.Succeeded -> SourceStatus(SourceStatusKind.UpToDate, stringResource(Res.string.source_status_up_to_date), modifier)
        is SourceFetchStatus.Failed -> SourceStatus(SourceStatusKind.Failed, status.message, modifier)
        is SourceFetchStatus.Stopped -> SourceStatus(SourceStatusKind.Stopped, stringResource(Res.string.source_status_stopped), modifier)
    }
}

/** Whether S1 lists the source under "Need you": its last run failed, or it's waiting for a code. */
internal fun SourceFetchStatus?.needsUser(): Boolean =
    this is SourceFetchStatus.Failed || (this is SourceFetchStatus.Running && step.isWaitingForUser())

/**
 * The scripts [query] matches, in their original order: those whose name,
 * short description or description contains every word of it, ignoring
 * case and accents ("aesio" finds "Aésio"). A blank query matches all.
 */
internal fun filterScripts(scripts: List<FydeScript>, query: String): List<FydeScript> {
    val words = foldForSearch(query).split(' ').filter { it.isNotBlank() }
    if (words.isEmpty()) return scripts
    return scripts.filter { script ->
        val haystack = foldForSearch("${script.name} ${script.shortDescription} ${script.description}")
        words.all { it in haystack }
    }
}

/** [text] lowercased, with the Latin accents scripts' names and descriptions use (French, mostly) folded away. */
private fun foldForSearch(text: String): String = buildString(text.length) {
    text.lowercase().forEach { char ->
        append(
            when (char) {
                'à', 'á', 'â', 'ã', 'ä', 'å' -> "a"
                'æ' -> "ae"
                'ç' -> "c"
                'è', 'é', 'ê', 'ë' -> "e"
                'ì', 'í', 'î', 'ï' -> "i"
                'ñ' -> "n"
                'ò', 'ó', 'ô', 'õ', 'ö' -> "o"
                'œ' -> "oe"
                'ù', 'ú', 'û', 'ü' -> "u"
                'ý', 'ÿ' -> "y"
                else -> char.toString()
            },
        )
    }
}

/**
 * [icon]'s image content never fills [ScriptIcon]'s full chip — publisher
 * logos arrive in wildly inconsistent shapes/paddings, so every logo is
 * inset the same amount and the chip reads as a uniform frame.
 */
private const val ICON_IMAGE_SCALE = 0.72f
private const val ICON_GLYPH_SCALE = 0.45f

/**
 * A neutral chip showing [icon] (the script's own PNG logo, see
 * [FydeScript.icon]), decoded via [decodeIconBitmap]. Falls back to a drawn
 * [PuzzleGlyph] when [icon] is empty or isn't valid image data.
 */
@Composable
internal fun ScriptIcon(icon: ByteArray = ByteArray(0), modifier: Modifier = Modifier, size: Dp = AppDimensions.iconTile) {
    val bitmap = remember(icon) { decodeIconBitmap(icon) }
    AppIconTile(modifier = modifier, size = size) {
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
