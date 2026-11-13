package com.peltoche.fyde

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
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import com.peltoche.fyde.components.action.AppBackButton
import com.peltoche.fyde.components.action.AppButton
import com.peltoche.fyde.components.action.AppButtonVariant
import com.peltoche.fyde.components.action.AppCircleButton
import com.peltoche.fyde.components.action.AppTextButton
import com.peltoche.fyde.components.content.AppCard
import com.peltoche.fyde.components.content.AppDivider
import com.peltoche.fyde.components.content.AppGroup
import com.peltoche.fyde.components.content.AppGroupRow
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
import com.peltoche.fyde.components.navigation.AppPageBar
import com.peltoche.fyde.components.overlay.AppDialog
import com.peltoche.fyde.icons.AlertGlyph
import com.peltoche.fyde.icons.ChevronRightGlyph
import com.peltoche.fyde.icons.InfoGlyph
import com.peltoche.fyde.icons.PlusGlyph
import com.peltoche.fyde.icons.SearchGlyph
import com.peltoche.fyde.icons.StopGlyph
import com.peltoche.fyde.icons.SyncGlyph
import com.peltoche.fyde.layout.AppContentContainer
import com.peltoche.fyde.layout.AppContentWidth
import com.peltoche.fyde.layout.AppWindowSizeClass
import com.peltoche.fyde.layout.LocalAppWindowSizeClass
import com.peltoche.fyde.resources.Res
import com.peltoche.fyde.resources.cancel
import com.peltoche.fyde.resources.catalogue_accounts
import com.peltoche.fyde.resources.catalogue_add
import com.peltoche.fyde.resources.catalogue_add_account
import com.peltoche.fyde.resources.catalogue_added
import com.peltoche.fyde.resources.catalogue_all
import com.peltoche.fyde.resources.catalogue_limit
import com.peltoche.fyde.resources.catalogue_own
import com.peltoche.fyde.resources.catalogue_search_placeholder
import com.peltoche.fyde.resources.connect_failed_title
import com.peltoche.fyde.resources.connect_source_another_submit
import com.peltoche.fyde.resources.connect_source_encrypted
import com.peltoche.fyde.resources.connecting_message
import com.peltoche.fyde.resources.document_detail_value_unset
import com.peltoche.fyde.resources.retry
import com.peltoche.fyde.resources.script_parameter_hidden
import com.peltoche.fyde.resources.script_parameter_no
import com.peltoche.fyde.resources.script_parameter_yes
import com.peltoche.fyde.resources.script_run_button
import com.peltoche.fyde.resources.script_save_button
import com.peltoche.fyde.resources.source_account_name
import com.peltoche.fyde.resources.source_add_account
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
import com.peltoche.fyde.resources.source_folder_change
import com.peltoche.fyde.resources.source_folder_label
import com.peltoche.fyde.resources.source_last_run
import com.peltoche.fyde.resources.source_other_accounts
import com.peltoche.fyde.resources.source_remove
import com.peltoche.fyde.resources.source_remove_account
import com.peltoche.fyde.resources.source_remove_account_text
import com.peltoche.fyde.resources.source_remove_account_title
import com.peltoche.fyde.resources.source_remove_confirm
import com.peltoche.fyde.resources.source_remove_text
import com.peltoche.fyde.resources.source_remove_title
import com.peltoche.fyde.resources.source_rename
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
     * own), `null` while it's fetched; [installations] the accounts the user
     * has connected, one or more per source (design-system.md § Accounts of
     * a source). An id in [pendingIds] — an installation's, or a script's
     * while one more account of it is being connected — has an install or
     * removal in flight; [errors] says, by the same ids, why it failed.
     * [folders] are the vault's, for where each account files its documents
     * (design-system.md § A source's folder).
     */
    data class Loaded(
        val scripts: List<FydeScript>?,
        val installations: List<FydeInstallation> = emptyList(),
        val quota: SourceQuota? = null,
        val pendingIds: Set<String> = emptySet(),
        val errors: Map<String, String> = emptyMap(),
        val folders: VaultFolders = VaultFolders(),
    ) : SourcesTabUiState {
        /** Every connected account, by source then name. */
        val accounts: List<SourceAccount> = sourceAccountsOf(scripts.orEmpty(), installations)

        /** [scriptId]'s accounts, by name. */
        fun accountsOf(scriptId: String): List<SourceAccount> = accounts.filter { it.script.id == scriptId }

        /** Whether the plan leaves no room for one more source (more accounts of a connected one always fit). */
        val atLimit: Boolean get() = quota != null && !quota.allowsAnother(0)
    }
}

/**
 * Which source page is open over the list. At Compact and Medium each is a
 * page of its own; at Expanded [Connect] fills the main area and the others
 * open in the detail panel, [Fetching] reading there as [Detail] (its
 * "Last run" stepper shows the same run). Every page but [Connect] is about
 * one account, by installation id.
 */
sealed interface SourceFocus {
    /** S5 (S5c when its source has several accounts), or S6 while its last run failed. */
    data class Detail(val installationId: String) : SourceFocus

    /** S3, or S3b when [scriptId] already has accounts. */
    data class Connect(val scriptId: String) : SourceFocus

    /** S4. */
    data class Fetching(val installationId: String) : SourceFocus

    /** S5's Identifiants (and Nom du compte), edited. */
    data class Credentials(val installationId: String) : SourceFocus
}

/** The account a page is about, if [this] is about one. */
private val SourceFocus.installationId: String?
    get() = when (this) {
        is SourceFocus.Detail -> installationId
        is SourceFocus.Fetching -> installationId
        is SourceFocus.Credentials -> installationId
        is SourceFocus.Connect -> null
    }

/**
 * The Sources tab: S1–S7 as pages at Compact and Medium, D4 at Expanded
 * (a sortable table beside a detail panel), one row per account. Statuses
 * come from this app's own runs ([statuses], by installation id);
 * `fyde-sdk` keeps no run history,
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
    onConnect: (FydeScript, label: String, Map<String, FydeScriptParameterValue>, folderId: String?) -> Unit,
    onUpdate: (SourceAccount, label: String, Map<String, FydeScriptParameterValue>, rerun: Boolean) -> Unit,
    onChangeFolder: (SourceAccount, folderId: String?) -> Unit,
    createFolder: CreateFolder,
    onRun: (SourceAccount) -> Unit,
    onStop: (SourceAccount) -> Unit,
    onRemove: (SourceAccount) -> Unit,
    onOpenPlan: () -> Unit,
    navigation: TopLevelNavigation,
    modifier: Modifier = Modifier,
) {
    var removing by remember { mutableStateOf<SourceAccount?>(null) }
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
                    onChangeFolder = onChangeFolder,
                    createFolder = createFolder,
                    onRun = onRun,
                    onStop = onStop,
                    onRemove = { removing = it },
                    onOpenPlan = onOpenPlan,
                )
                CompositionLocalProvider(LocalVaultFolders provides state.folders) {
                    if (windowSizeClass == AppWindowSizeClass.Expanded) {
                        WideSourcesTab(tab, catalogueOpen, pageModifier)
                    } else {
                        NarrowSourcesTab(tab, catalogueOpen, pageModifier)
                    }
                }
            }
        }
    }

    removing?.let { account ->
        // S7b for one account among several, S7 for a source's last one.
        val name = account.script.name
        AppDialog(
            onDismissRequest = { removing = null },
            title = if (account.shared) {
                stringResource(Res.string.source_remove_account_title, name, account.label)
            } else {
                stringResource(Res.string.source_remove_title, name)
            },
            text = if (account.shared) {
                stringResource(Res.string.source_remove_account_text, name)
            } else {
                stringResource(Res.string.source_remove_text, name)
            },
            confirmLabel = stringResource(if (account.shared) Res.string.source_remove_account else Res.string.source_remove_confirm),
            onConfirm = {
                removing = null
                onRemove(account)
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
    val onConnect: (FydeScript, String, Map<String, FydeScriptParameterValue>, String?) -> Unit,
    val onUpdate: (SourceAccount, String, Map<String, FydeScriptParameterValue>, Boolean) -> Unit,
    val onChangeFolder: (SourceAccount, String?) -> Unit,
    val createFolder: CreateFolder,
    val onRun: (SourceAccount) -> Unit,
    val onStop: (SourceAccount) -> Unit,
    val onRemove: (SourceAccount) -> Unit,
    val onOpenPlan: () -> Unit,
) {
    fun script(id: String): FydeScript? = state.scripts?.find { it.id == id }

    fun account(installationId: String): SourceAccount? = state.accounts.find { it.installationId == installationId }

    fun isPending(id: String): Boolean = id in state.pendingIds

    /** Whether [installationId] has a run in progress; other accounts' runs don't matter (runs are parallel). */
    fun isRunning(installationId: String): Boolean = installationId in runningIds

    /** Whether [installationId]'s run was asked to stop and hasn't ended yet. */
    fun isStopping(installationId: String): Boolean = installationId in stoppingIds
}

/** Compact and Medium: one page at a time, S1 at the bottom of the stack. */
@Composable
private fun NarrowSourcesTab(tab: SourcesTab, catalogueOpen: Boolean, modifier: Modifier) {
    val focus = tab.focus
    val connecting = (focus as? SourceFocus.Connect)?.let { tab.script(it.scriptId) }
    val account = focus?.installationId?.let(tab::account)
    AppContentContainer(modifier = modifier, width = AppContentWidth.Detail) {
        when {
            connecting != null -> key(connecting.id) {
                ConnectPage(tab, connecting, onBack = { tab.onFocusChange(null) })
            }
            focus is SourceFocus.Fetching && account != null -> key(account.installationId) { FetchingPage(tab, account) }
            focus is SourceFocus.Credentials && account != null -> key(account.installationId) {
                CredentialsPage(tab, account, onBack = { tab.onFocusChange(SourceFocus.Detail(account.installationId)) })
            }
            account != null -> key(account.installationId) { DetailPage(tab, account) }
            catalogueOpen -> CataloguePage(tab, onBack = { tab.onCatalogueOpenChange(false) })
            else -> SourceListPage(tab)
        }
    }
}

/**
 * D4: the table (or the catalogue, or a source's connect form, in its place)
 * beside a detail panel flush to the window's right edge, showing the
 * selected account — the first one until another is picked.
 */
@Composable
private fun WideSourcesTab(tab: SourcesTab, catalogueOpen: Boolean, modifier: Modifier) {
    val focus = tab.focus
    val connecting = (focus as? SourceFocus.Connect)?.let { tab.script(it.scriptId) }
    val selected = focus?.installationId?.let(tab::account) ?: tab.state.accounts.firstOrNull()
    Row(modifier = modifier) {
        Box(modifier = Modifier.weight(1f).fillMaxHeight()) {
            when {
                connecting != null -> AppContentContainer(width = AppContentWidth.Detail) {
                    key(connecting.id) { ConnectPage(tab, connecting, onBack = { tab.onFocusChange(null) }) }
                }
                catalogueOpen -> AppContentContainer(width = AppContentWidth.Detail) {
                    CataloguePage(tab, onBack = { tab.onCatalogueOpenChange(false) })
                }
                else -> AppContentContainer(width = AppContentWidth.List) { SourceTablePage(tab, selected?.installationId) }
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
                key(selected.installationId) {
                    Column(modifier = Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(AppSpacing.space6)) {
                        if (focus is SourceFocus.Credentials && focus.installationId == selected.installationId) {
                            AppBackButton(onClick = { tab.onFocusChange(SourceFocus.Detail(selected.installationId)) })
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

/** S1: the accounts that need the user first, then the others, and the plan's count. */
@Composable
private fun SourceListPage(tab: SourcesTab) {
    val installed = tab.state.accounts
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
                val needsYou = installed.filter { tab.statuses[it.installationId].needsUser() }
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
private fun SourceRows(tab: SourcesTab, accounts: List<SourceAccount>, showSource: Boolean = true) {
    AppGroup {
        accounts.forEachIndexed { index, account ->
            if (index > 0) AppDivider()
            SourceRow(
                account = account,
                status = tab.statuses[account.installationId],
                showSource = showSource,
                onClick = { tab.onFocusChange(SourceFocus.Detail(account.installationId)) },
            )
        }
    }
}

/**
 * One S1 row: tile, name ("Ameli · Marie" once the source has several
 * accounts; the label alone under S5c's "Other accounts", [showSource]
 * false), status (or what it fetches until it has one), chevron.
 */
@Composable
private fun SourceRow(account: SourceAccount, status: SourceFetchStatus?, onClick: () -> Unit, showSource: Boolean = true) {
    AppGroupRow(onClick = onClick, minHeight = AppDimensions.rowHeightComfortable) {
        ScriptIcon(icon = account.script.icon)
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = if (showSource) accountName(account) else AnnotatedString(account.label),
                style = AppTypography.bodyEmphasis,
                color = AppColors.text,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
            if (status != null) {
                SourceStatusLine(status)
            } else {
                Summary(account.script)
            }
        }
        ChevronRightGlyph(modifier = Modifier.size(AppIconSize.sm))
    }
}

/**
 * [account]'s name as rows and titles show it: the source's, then " · " and
 * the account's in `textMuted`, regular weight, once the source has several
 * accounts (design-system.md § Accounts of a source).
 */
@Composable
private fun accountName(account: SourceAccount): AnnotatedString {
    val muted = AppColors.textMuted
    return buildAnnotatedString {
        append(account.script.name)
        if (account.shared) {
            withStyle(SpanStyle(color = muted, fontWeight = FontWeight.Normal)) {
                append(ACCOUNT_SEPARATOR)
                append(account.label)
            }
        }
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
    val rows = remember(tab.state.accounts, tab.statuses, sort) { sortedSources(tab.state.accounts, tab.statuses, sort) }
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
                rows.forEach { account ->
                    SourceTableRow(
                        account = account,
                        status = tab.statuses[account.installationId],
                        selected = account.installationId == selectedId,
                        onClick = { tab.onFocusChange(SourceFocus.Detail(account.installationId)) },
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
private fun SourceTableRow(account: SourceAccount, status: SourceFetchStatus?, selected: Boolean, onClick: () -> Unit) {
    AppGroupRow(
        onClick = onClick,
        minHeight = AppDimensions.rowHeightStandard,
        background = if (selected) AppColors.selectionSurface else Color.Transparent,
        horizontalPadding = AppSpacing.space4,
    ) {
        Row(modifier = Modifier.weight(1f), verticalAlignment = Alignment.CenterVertically) {
            ScriptIcon(icon = account.script.icon, size = AppDimensions.iconTileSmall)
            Spacer(Modifier.width(AppSpacing.space4))
            Text(
                text = accountName(account),
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
    sources: List<SourceAccount>,
    statuses: Map<String, SourceFetchStatus>,
    sort: SourceSort,
): List<SourceAccount> {
    val byName = compareBy<SourceAccount>({ it.script.name.lowercase() }, { it.label.lowercase() })
    val comparator = when (sort.column) {
        SourceColumn.Name -> byName
        SourceColumn.Status -> compareBy<SourceAccount> { statusRank(statuses[it.installationId]) }.then(byName)
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
        AppPageBar(title = stringResource(Res.string.sources_tab_add), onBack = onBack)
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
            val accounts = tab.state.accountsOf(script.id).size
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
                    when {
                        accounts > 1 -> Text(
                            text = stringResource(Res.string.catalogue_accounts, accounts),
                            style = AppTypography.caption,
                            color = AppColors.textMuted,
                        )
                        accounts == 1 -> Text(text = stringResource(Res.string.catalogue_added), style = AppTypography.caption, color = AppColors.textMuted)
                        else -> Summary(script, maxLines = 2)
                    }
                }
                // A source already added takes one more account (S3b), which
                // the plan doesn't count: never dimmed at its limit.
                AppButton(
                    text = stringResource(if (accounts > 0) Res.string.catalogue_add_account else Res.string.catalogue_add),
                    onClick = { tab.onFocusChange(SourceFocus.Connect(script.id)) },
                    variant = AppButtonVariant.Outlined,
                    enabled = accounts > 0 || !tab.state.atLimit,
                )
            }
        }
    }
}

// S3 ------------------------------------------------------------------------------------------

/**
 * S3: what the source fetches, the sites it may reach, its fields, Ranger
 * dans (Rangement automatique until changed; S3c's À classer in a vault
 * with no folder); the connect button stays pinned at the bottom. S3b once
 * the source has accounts: titled as another one, naming those, with Nom du
 * compte, and Ranger dans starting from its first account's folder.
 */
@Composable
private fun ConnectPage(tab: SourcesTab, script: FydeScript, onBack: () -> Unit) {
    val values = remember { mutableStateMapOf<String, String>() }
    val accounts = tab.state.accountsOf(script.id)
    val others = accounts.map { it.label }
    val accountName = remember { mutableStateOf("") }
    var showRequiredErrors by remember { mutableStateOf(false) }
    val first = remember { accounts.firstOrNull() }
    var folderId by remember { mutableStateOf(first?.folderId) }
    var folderChanged by remember { mutableStateOf(false) }
    var folderSheet by remember { mutableStateOf<SourceFolderSheet?>(null) }
    val form = ConnectSourceForm(
        script = script,
        values = values,
        showRequiredErrors = showRequiredErrors,
        errorMessage = tab.state.errors[script.id],
        isSubmitting = tab.isPending(script.id),
        onSubmit = {
            val entered = validatedParameterValues(script.parameters, values)
            // A folder deleted meanwhile files automatically, as the row reads.
            val folder = folderId?.takeIf { tab.state.folders[it] != null }
            if (entered != null) tab.onConnect(script, accountName.value, entered, folder) else showRequiredErrors = true
        },
        accountName = accountName.takeIf { others.isNotEmpty() },
        otherAccounts = others.takeIf { it.isNotEmpty() },
        folder = SourceFolderPick(
            folderId = folderId,
            sameAs = first?.label?.takeIf { !folderChanged },
            onOpen = { folderSheet = it },
        ),
    )
    SourceFolderSheets(
        sheet = folderSheet,
        onSheetChange = { folderSheet = it },
        accountName = script.name,
        sourceName = script.name,
        current = folderId,
        onChoose = {
            folderId = it
            folderChanged = true
        },
        createFolder = tab.createFolder,
    )
    Column(modifier = Modifier.fillMaxSize()) {
        AppPageBar(title = null, onBack = onBack, enabled = !form.isSubmitting)
        Column(modifier = Modifier.weight(1f).fillMaxWidth().verticalScroll(rememberScrollState())) {
            Spacer(Modifier.height(AppSpacing.space4))
            ConnectSourceForm(form, encryptedNote = Res.string.connect_source_encrypted)
            Spacer(Modifier.height(AppSpacing.space6))
        }
        Box(modifier = Modifier.fillMaxWidth().padding(top = AppSpacing.space4, bottom = AppSpacing.space5)) {
            if (others.isEmpty()) ConnectButton(form) else ConnectButton(form, text = stringResource(Res.string.connect_source_another_submit))
        }
    }
}

// S4 ------------------------------------------------------------------------------------------

/** S4: the run as a stepper; it carries on in the background whatever the user does next. */
@Composable
private fun FetchingPage(tab: SourcesTab, account: SourceAccount) {
    val script = account.script
    val id = account.installationId
    val status = tab.statuses[id] ?: SourceFetchStatus.Queued
    val over = status is SourceFetchStatus.Succeeded || status is SourceFetchStatus.Failed || status is SourceFetchStatus.Stopped
    val leave = { tab.onFocusChange(null) }
    Column(modifier = Modifier.fillMaxSize()) {
        AppPageBar(title = account.name, onBack = leave)
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
                    onClick = { tab.onStop(account) },
                    loading = tab.isStopping(id),
                    enabled = !tab.isStopping(id),
                    leadingIcon = { StopGlyph(modifier = Modifier.size(AppIconSize.sm), tint = AppColors.primary) },
                    modifier = Modifier.fillMaxWidth(),
                )
                status is SourceFetchStatus.Stopped -> AppTextButton(
                    text = stringResource(Res.string.source_rerun),
                    onClick = { tab.onRun(account) },
                    enabled = !tab.isRunning(id),
                    leadingIcon = { SyncGlyph(modifier = Modifier.size(AppIconSize.sm), tint = AppColors.primary) },
                    modifier = Modifier.fillMaxWidth(),
                )
            }
        }
    }
}

// S5, S6 --------------------------------------------------------------------------------------

@Composable
private fun DetailPage(tab: SourcesTab, account: SourceAccount) {
    val failed = tab.statuses[account.installationId] is SourceFetchStatus.Failed
    Column(modifier = Modifier.fillMaxSize().verticalScroll(rememberScrollState())) {
        AppPageBar(
            title = null,
            onBack = { tab.onFocusChange(null) },
            trailing = if (failed) null else { { RunButton(tab, account) } },
        )
        Spacer(Modifier.height(AppSpacing.space3))
        DetailBody(tab, account, wide = false)
        Spacer(Modifier.height(AppSpacing.space7))
    }
}

/**
 * S5 (or S6 once its last run failed): the header, the last run of this app
 * session, Réglages, Ajouter un autre compte, Retirer la source. S5c when its
 * source has several accounts: "Ameli · Marie", Nom du compte first in
 * Réglages, the source's other accounts, Retirer ce compte. In D4's panel
 * ([wide]) Lancer sits beside the header and Identifiants opens with
 * Modifier rather than a chevron.
 */
@Composable
private fun ColumnScope.DetailBody(tab: SourcesTab, account: SourceAccount, wide: Boolean) {
    val script = account.script
    val id = account.installationId
    val status = tab.statuses[id]
    Row(verticalAlignment = Alignment.CenterVertically) {
        ScriptIcon(icon = script.icon, size = if (wide) AppDimensions.iconTileHeader else AppDimensions.iconTileLarge)
        Spacer(Modifier.width(AppSpacing.space4))
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = accountName(account),
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
            RunButton(tab, account)
        }
    }

    if (status is SourceFetchStatus.Failed) {
        Spacer(Modifier.height(AppSpacing.space5))
        AppErrorNote(status.message)
        Spacer(Modifier.height(AppSpacing.space5))
        UpdateForm(tab, account, autofocus = !wide)
    } else {
        if (status != null) {
            AppSectionTitle(stringResource(Res.string.source_last_run))
            AppCard(modifier = Modifier.fillMaxWidth(), outlined = true) {
                RunStepper(steps = sourceFetchSteps(status))
            }
        }
        SourceSettings(tab, account, wide)
    }

    val others = tab.state.accountsOf(script.id).filter { it.installationId != id }
    if (others.isNotEmpty()) {
        AppSectionTitle(stringResource(Res.string.source_other_accounts, script.name))
        SourceRows(tab, others, showSource = false)
    }

    Spacer(Modifier.height(AppSpacing.space6))
    AppTextButton(
        text = stringResource(Res.string.source_add_account, script.name),
        onClick = { tab.onFocusChange(SourceFocus.Connect(script.id)) },
        enabled = !tab.isPending(script.id),
        leadingIcon = { PlusGlyph(modifier = Modifier.size(AppIconSize.sm), tint = AppColors.primary) },
        modifier = Modifier.fillMaxWidth(),
    )
    Spacer(Modifier.height(AppSpacing.space2))
    AppButton(
        text = stringResource(if (account.shared) Res.string.source_remove_account else Res.string.source_remove),
        onClick = { tab.onRemove(account) },
        variant = AppButtonVariant.Outlined,
        destructive = true,
        enabled = !tab.isPending(id),
        modifier = Modifier.fillMaxWidth(),
    )
}

/**
 * Lancer, turned into an outlined Arrêter while this account runs (S5b, D4b),
 * busy until the stopped run has really ended; other accounts' runs go in
 * parallel.
 */
@Composable
private fun RunButton(tab: SourcesTab, account: SourceAccount) {
    val id = account.installationId
    if (tab.isRunning(id)) {
        val stopping = tab.isStopping(id)
        AppButton(
            text = stringResource(Res.string.script_stop_button),
            onClick = { tab.onStop(account) },
            variant = AppButtonVariant.Outlined,
            loading = stopping,
            enabled = !stopping,
            leadingIcon = { StopGlyph(modifier = Modifier.size(AppIconSize.sm), tint = AppColors.text) },
        )
    } else {
        AppButton(
            text = stringResource(Res.string.script_run_button),
            onClick = { tab.onRun(account) },
            variant = AppButtonVariant.Outlined,
            enabled = !tab.isPending(id),
            leadingIcon = { SyncGlyph(modifier = Modifier.size(AppIconSize.sm), tint = AppColors.text) },
        )
    }
}

/**
 * Réglages: Nom du compte once the source has several accounts, Ranger dans
 * (S9, or D12 in D4's panel: changing it reinstalls the account with the
 * new folder), the credentials it signs in with (first plain value shown)
 * and the sites it may reach. Nom du compte and Identifiants both open the
 * credentials form.
 */
@Composable
private fun SourceSettings(tab: SourcesTab, account: SourceAccount, wide: Boolean) {
    val script = account.script
    val id = account.installationId
    val hasCredentials = script.parameters.isNotEmpty()
    val openCredentials = { tab.onFocusChange(SourceFocus.Credentials(id)) }
    var folderSheet by remember(id) { mutableStateOf<SourceFolderSheet?>(null) }
    AppSectionTitle(stringResource(Res.string.source_settings))
    AppGroup {
        if (account.shared) {
            EditableSettingRow(
                label = stringResource(Res.string.source_account_name),
                value = account.label,
                action = stringResource(Res.string.source_rename),
                wide = wide,
                enabled = !tab.isPending(id),
                onClick = openCredentials,
            )
            AppDivider()
        }
        EditableSettingRow(
            label = stringResource(Res.string.source_folder_label),
            value = sourceFolderLabel(account.folderId),
            action = stringResource(Res.string.source_folder_change),
            wide = wide,
            enabled = !tab.isPending(id),
            onClick = { folderSheet = SourceFolderSheet.Pick },
        )
        if (hasCredentials) {
            AppDivider()
            EditableSettingRow(
                label = stringResource(Res.string.source_credentials),
                value = credentialsSummary(script, account.parameters),
                action = stringResource(Res.string.source_edit),
                wide = wide,
                enabled = !tab.isPending(id),
                onClick = openCredentials,
            )
        }
        if (script.allowedDomains.isNotEmpty()) {
            AppDivider()
            SettingRow(label = stringResource(Res.string.source_sites), value = script.allowedDomains.joinToString(", "))
        }
    }
    SourceFolderSheets(
        sheet = folderSheet,
        onSheetChange = { folderSheet = it },
        accountName = account.name,
        sourceName = script.name,
        current = account.folderId?.takeIf { tab.state.folders[it] != null },
        onChoose = { tab.onChangeFolder(account, it) },
        createFolder = tab.createFolder,
    )
}

/** A Réglages row opening a form or sheet: with [action] in D4's panel ([wide]), as a chevron row elsewhere. */
@Composable
private fun EditableSettingRow(label: String, value: String, action: String, wide: Boolean, enabled: Boolean, onClick: () -> Unit) {
    if (wide) {
        SettingRow(label = label, value = value) {
            AppTextButton(text = action, onClick = onClick, enabled = enabled)
        }
    } else {
        AppGroupRow(onClick = onClick, minHeight = AppDimensions.rowHeightStandard, enabled = enabled) {
            SettingText(label = label, value = value, modifier = Modifier.weight(1f))
            ChevronRightGlyph(modifier = Modifier.size(AppIconSize.sm))
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
private fun UpdateForm(tab: SourcesTab, account: SourceAccount, autofocus: Boolean) {
    if (account.script.parameters.isEmpty()) {
        AppButton(
            text = stringResource(Res.string.source_rerun),
            onClick = { tab.onRun(account) },
            enabled = !tab.isRunning(account.installationId),
            modifier = Modifier.fillMaxWidth(),
        )
        return
    }
    CredentialsFields(
        tab = tab,
        account = account,
        submitLabel = stringResource(Res.string.source_update_rerun),
        rerun = true,
        autofocus = autofocus,
    )
}

// Credentials ---------------------------------------------------------------------------------

@Composable
private fun CredentialsPage(tab: SourcesTab, account: SourceAccount, onBack: () -> Unit) {
    Column(modifier = Modifier.fillMaxSize().verticalScroll(rememberScrollState())) {
        AppPageBar(title = account.name, onBack = onBack, enabled = !tab.isPending(account.installationId))
        Spacer(Modifier.height(AppSpacing.space3))
        CredentialsBody(tab, account, autofocus = true)
        Spacer(Modifier.height(AppSpacing.space7))
    }
}

@Composable
private fun CredentialsBody(tab: SourcesTab, account: SourceAccount, autofocus: Boolean) {
    Text(
        text = stringResource(Res.string.source_credentials),
        style = AppTypography.title,
        color = AppColors.text,
        modifier = Modifier.semantics { heading() },
    )
    Spacer(Modifier.height(AppSpacing.space5))
    CredentialsFields(
        tab = tab,
        account = account,
        submitLabel = stringResource(Res.string.script_save_button),
        rerun = false,
        autofocus = autofocus,
    )
}

/**
 * An account's fields prefilled with what it's installed with, secrets left
 * blank (a blank one keeps the installed value), its name once its source
 * has several accounts, and [submitLabel] reinstalling it — then running it
 * again when [rerun].
 */
@Composable
private fun CredentialsFields(
    tab: SourcesTab,
    account: SourceAccount,
    submitLabel: String,
    rerun: Boolean,
    autofocus: Boolean,
) {
    val script = account.script
    val id = account.installationId
    val installed = account.parameters
    val keptSecrets = remember(installed) { keptSecretKeys(script.parameters, installed) }
    val values = remember {
        mutableStateMapOf<String, String>().apply {
            putAll(formValues(installed.filterKeys { script.parameters[it]?.secret == false }))
        }
    }
    val accountName = remember { mutableStateOf(account.label) }
    var showRequiredErrors by remember { mutableStateOf(false) }
    val form = ConnectSourceForm(
        script = script,
        values = values,
        showRequiredErrors = showRequiredErrors,
        errorMessage = tab.state.errors[id],
        isSubmitting = tab.isPending(id),
        keptSecrets = keptSecrets,
        // Rerunning needs this account's run over: ScraperRuns won't start a
        // second one, so the new credentials would only be saved.
        canSubmit = !(rerun && tab.isRunning(id)),
        onSubmit = {
            val entered = validatedCredentials(script.parameters, values, installed)
            if (entered != null) tab.onUpdate(account, accountName.value, entered, rerun) else showRequiredErrors = true
        },
        accountName = accountName.takeIf { account.shared },
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
