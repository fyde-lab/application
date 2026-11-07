package com.peltoche.fyde

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeContentPadding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListScope
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.selection.toggleable
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.LocalMinimumInteractiveComponentSize
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import com.peltoche.fyde.components.action.AppButton
import com.peltoche.fyde.components.action.AppButtonVariant
import com.peltoche.fyde.components.action.AppTextButton
import com.peltoche.fyde.components.content.AppDivider
import com.peltoche.fyde.components.feedback.AppNote
import com.peltoche.fyde.components.feedback.AppNoteKind
import com.peltoche.fyde.components.feedback.AppPlanQuota
import com.peltoche.fyde.components.form.AppCheckbox
import com.peltoche.fyde.components.form.AppSearchField
import com.peltoche.fyde.icons.AlertGlyph
import com.peltoche.fyde.icons.InfoGlyph
import com.peltoche.fyde.icons.SearchGlyph
import com.peltoche.fyde.layout.AppWindowSizeClass
import com.peltoche.fyde.layout.CenteredScrollColumn
import com.peltoche.fyde.layout.LocalAppWindowSizeClass
import com.peltoche.fyde.resources.Res
import com.peltoche.fyde.resources.plan_quota_description
import com.peltoche.fyde.resources.plan_quota_short
import com.peltoche.fyde.resources.plan_quota_short_full
import com.peltoche.fyde.resources.plan_quota_wide
import com.peltoche.fyde.resources.plan_quota_wide_full
import com.peltoche.fyde.resources.retry
import com.peltoche.fyde.resources.sources_connect
import com.peltoche.fyde.resources.sources_connect_none
import com.peltoche.fyde.resources.sources_empty
import com.peltoche.fyde.resources.sources_limit_compact
import com.peltoche.fyde.resources.sources_limit_compact_more
import com.peltoche.fyde.resources.sources_limit_wide
import com.peltoche.fyde.resources.sources_load_failed
import com.peltoche.fyde.resources.sources_no_match
import com.peltoche.fyde.resources.sources_search_clear
import com.peltoche.fyde.resources.sources_search_placeholder
import com.peltoche.fyde.resources.sources_skip
import com.peltoche.fyde.resources.sources_subtitle
import com.peltoche.fyde.resources.sources_title
import com.peltoche.fyde.resources.vault_step_sources_title
import com.peltoche.fyde.theme.AppColors
import com.peltoche.fyde.theme.AppDimensions
import com.peltoche.fyde.theme.AppIconSize
import com.peltoche.fyde.theme.AppSpacing
import com.peltoche.fyde.theme.AppTypography
import org.jetbrains.compose.resources.pluralStringResource
import org.jetbrains.compose.resources.stringResource

/**
 * The plan's source limit, as [SourcesScreen]'s counter reads it: the
 * [active] sources already enabled on the account, out of [max]. `App.kt`
 * builds it from [FydeSdk.getUsage] ([sourceQuotaOf]).
 */
data class SourceQuota(val planId: String, val active: Int, val max: Int) {
    /** Sources used once [selected] more are added to the [active] ones. */
    fun used(selected: Int): Int = active + selected

    /** Whether one more source fits beside [selected] ticked ones. */
    fun allowsAnother(selected: Int): Boolean = used(selected) < max
}

/**
 * The plan's source limit, or `null` when it has none (`maxScripts` ≤ 0),
 * which hides the counter.
 */
fun sourceQuotaOf(usage: FydePlanUsage): SourceQuota? =
    if (usage.maxScripts <= 0) null else SourceQuota(usage.planId, usage.enabledScripts, usage.maxScripts)

/** What [SourcesScreen] shows while `App.kt` lists the public scrapers. */
sealed interface SourcesUiState {
    data object Loading : SourcesUiState

    /**
     * The scrapers on offer and the plan's [quota] — `null` when the plan has
     * no limit or [FydeSdk.getUsage] failed, which hides the counter (the
     * server's own refusal then shows when the source is connected).
     */
    data class Loaded(val sources: List<FydeScript>, val quota: SourceQuota?) : SourcesUiState

    class Failed(val message: String, val onRetry: () -> Unit) : SourcesUiState
}

/**
 * Choisir ses sources, the vault-creation step after the recovery kit: the
 * public scrapers to tick, within the plan's limit, before each is
 * connected in turn. Pure presentation: `App.kt` lists the scrapers, holds
 * [selected] (in ticking order, the order they'll be connected in) and
 * [query], and moves on through [onConnect] or [onSkip].
 *
 * At Compact it's O7 (`design/assets/screens/O7-sources.png`): the step dots
 * and "Passer" on top, the plan counter, the search, the sources as rows of
 * one card, the connect button pinned at the bottom. Once the title scrolls
 * away the bar names the step and the counter stays pinned under it (O7b).
 * At Medium and Expanded it's DO7
 * (`design/assets/screens/DO7-choisir-ses-sources.png`):
 * [VaultCreationSplitLayout] at its sources step beside the same content in
 * a 600dp column, the sources as two columns of cards scrolling in a
 * fixed-height box, so the connect button stays close under them.
 *
 * At the plan's limit (O7b, DO7h) the counter turns `warning`, unticked
 * sources dim and stop taking a tick, ticked ones stay editable so
 * unticking frees a slot at once, and a note says how to swap.
 *
 * Not drawn from the mockups: their back button (the recovery key would be
 * generated anew on the way back, voiding the one just saved, as in
 * [RecoveryKitScreen]) and their category chips (`fyde-sdk`'s scripts carry
 * no category yet).
 */
@Composable
fun SourcesScreen(
    state: SourcesUiState,
    selected: Set<String>,
    onToggle: (FydeScript) -> Unit,
    query: String,
    onQueryChange: (String) -> Unit,
    onConnect: () -> Unit,
    onSkip: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val content = SourcesContent(state, selected, onToggle, query, onQueryChange)
    when (LocalAppWindowSizeClass.current) {
        AppWindowSizeClass.Compact -> CompactSources(content, onConnect, onSkip, modifier)
        AppWindowSizeClass.Medium, AppWindowSizeClass.Expanded -> VaultCreationSplitLayout(
            currentStep = VaultCreationStep.Sources,
            modifier = modifier,
        ) { paneModifier ->
            WideSourcesPane(content, onConnect, onSkip, paneModifier)
        }
    }
}

/** [SourcesScreen]'s inputs, bundled so both layouts read them the same way. */
private class SourcesContent(
    val state: SourcesUiState,
    val selected: Set<String>,
    val onToggle: (FydeScript) -> Unit,
    val query: String,
    val onQueryChange: (String) -> Unit,
) {
    val quota: SourceQuota? = (state as? SourcesUiState.Loaded)?.quota

    /** Whether the selection fills the plan (O7b, DO7h). */
    val atLimit: Boolean = quota != null && !quota.allowsAnother(selected.size)

    /** Whether [script] can be ticked or unticked now: ticked ones always, unticked ones while the plan has room. */
    fun canToggle(script: FydeScript): Boolean = script.id in selected || !atLimit
}

/** O7 and O7b. */
@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun CompactSources(
    content: SourcesContent,
    onConnect: () -> Unit,
    onSkip: () -> Unit,
    modifier: Modifier,
) {
    val listState = rememberLazyListState()
    // The heading is the list's first item: once it's gone, the bar names the step (O7b).
    val titleScrolledAway by remember { derivedStateOf { listState.firstVisibleItemIndex > 0 } }
    Box(
        modifier = modifier
            .fillMaxSize()
            .background(AppColors.background)
            .safeContentPadding(),
        contentAlignment = Alignment.TopCenter,
    ) {
        Column(
            modifier = Modifier
                .widthIn(max = AppDimensions.maxWidthDetail)
                .fillMaxSize(),
        ) {
            CompactSourcesBar(titleScrolledAway, onSkip)
            LazyColumn(
                state = listState,
                contentPadding = PaddingValues(bottom = AppSpacing.space6),
                modifier = Modifier.weight(1f).fillMaxWidth(),
            ) {
                item(key = "heading") {
                    Column(modifier = Modifier.padding(horizontal = AppSpacing.space6)) {
                        Spacer(Modifier.height(AppSpacing.space4))
                        SourcesHeading()
                        Spacer(Modifier.height(AppSpacing.space6))
                    }
                }
                val quota = content.quota
                if (quota != null) {
                    stickyHeader(key = "quota") {
                        // Opaque, so the rows scroll under it rather than through it.
                        Box(
                            modifier = Modifier
                                .background(AppColors.background)
                                .padding(horizontal = AppSpacing.space6, vertical = AppSpacing.space2),
                        ) {
                            SourcesQuota(quota, content.selected.size, wide = false)
                        }
                    }
                    if (content.atLimit) {
                        item(key = "limit") {
                            Text(
                                text = pluralStringResource(
                                    Res.plurals.sources_limit_compact,
                                    quota.max,
                                    planName(quota.planId),
                                    quota.max,
                                ),
                                style = AppTypography.body,
                                color = AppColors.textMuted,
                                modifier = Modifier.padding(horizontal = AppSpacing.space6, vertical = AppSpacing.space2),
                            )
                        }
                    }
                }
                item(key = "search") {
                    Box(modifier = Modifier.padding(horizontal = AppSpacing.space6, vertical = AppSpacing.space3)) {
                        SourcesSearch(content)
                    }
                }
                compactSourceList(content)
            }
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = AppSpacing.space6)
                    .padding(top = AppSpacing.space4, bottom = AppSpacing.space5),
            ) {
                ConnectButton(content, onConnect)
            }
        }
    }
}

/**
 * O7's bar: the step dots in the middle, or the step's name once the
 * title has scrolled away (O7b), and "Passer" at the far end. The left
 * stays empty where the mockup has its back button.
 */
@Composable
private fun CompactSourcesBar(titleScrolledAway: Boolean, onSkip: () -> Unit) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = AppSpacing.space6)
            .padding(top = AppSpacing.space2)
            .heightIn(min = AppDimensions.backButton),
    ) {
        if (titleScrolledAway) {
            Text(
                text = stringResource(Res.string.vault_step_sources_title),
                style = AppTypography.section,
                color = AppColors.text,
                modifier = Modifier.align(Alignment.Center).semantics { heading() },
            )
        } else {
            VaultStepDots(VaultCreationStep.Sources, modifier = Modifier.align(Alignment.Center))
        }
        AppTextButton(
            text = stringResource(Res.string.sources_skip),
            onClick = onSkip,
            modifier = Modifier.align(Alignment.CenterEnd),
        )
    }
}

/** O7's sources: one card, a row per source split by hairlines; or why there's none to show. */
private fun LazyListScope.compactSourceList(content: SourcesContent) {
    when (val state = content.state) {
        SourcesUiState.Loading -> item(key = "loading") { SourcesLoading() }
        is SourcesUiState.Failed -> item(key = "failed") {
            Column(modifier = Modifier.padding(horizontal = AppSpacing.space6)) { SourcesFailed(state) }
        }
        is SourcesUiState.Loaded -> {
            val matches = filterScripts(state.sources, content.query)
            if (matches.isEmpty()) {
                item(key = "none") {
                    Box(modifier = Modifier.padding(horizontal = AppSpacing.space6)) { NoSources(content) }
                }
                return
            }
            item(key = "sources") {
                Column(
                    modifier = Modifier
                        .padding(horizontal = AppSpacing.space6)
                        .clip(SourceCardShape)
                        .background(AppColors.surface)
                        .border(AppDimensions.borderHairline, AppColors.border, SourceCardShape),
                ) {
                    matches.forEachIndexed { index, script ->
                        if (index > 0) AppDivider()
                        SourceRow(
                            script = script,
                            checked = script.id in content.selected,
                            enabled = content.canToggle(script),
                            onToggle = { content.onToggle(script) },
                        )
                    }
                }
            }
            if (content.atLimit) {
                item(key = "more") {
                    Text(
                        text = stringResource(Res.string.sources_limit_compact_more),
                        style = AppTypography.caption,
                        color = AppColors.textMuted,
                        modifier = Modifier.padding(horizontal = AppSpacing.space6, vertical = AppSpacing.space4),
                    )
                }
            }
        }
    }
}

/** DO7's pane: the content centred in a 600dp column, "Passer" at the top right. */
@Composable
private fun WideSourcesPane(
    content: SourcesContent,
    onConnect: () -> Unit,
    onSkip: () -> Unit,
    modifier: Modifier,
) {
    Box(modifier = modifier.background(AppColors.background).safeContentPadding()) {
        CenteredScrollColumn(
            modifier = Modifier.fillMaxSize(),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Column(
                modifier = Modifier
                    .widthIn(max = AppDimensions.authFormColumnWide)
                    .fillMaxWidth()
                    .padding(horizontal = AppSpacing.space7, vertical = AppSpacing.space10),
            ) {
                SourcesHeading()
                Spacer(Modifier.height(AppSpacing.space6))
                content.quota?.let { quota ->
                    SourcesQuota(quota, content.selected.size, wide = true)
                    Spacer(Modifier.height(AppSpacing.space4))
                }
                SourcesSearch(content)
                Spacer(Modifier.height(AppSpacing.space5))
                WideSourceGrid(content)
                if (content.atLimit) {
                    Spacer(Modifier.height(AppSpacing.space5))
                    AppNote(
                        message = stringResource(Res.string.sources_limit_wide),
                        kind = AppNoteKind.Warning,
                        icon = { tint -> InfoGlyph(modifier = Modifier.size(AppIconSize.md), tint = tint) },
                    )
                }
                Spacer(Modifier.height(AppSpacing.space6))
                ConnectButton(content, onConnect)
            }
        }
        AppTextButton(
            text = stringResource(Res.string.sources_skip),
            onClick = onSkip,
            modifier = Modifier
                .align(Alignment.TopEnd)
                .padding(end = AppSpacing.space9, top = AppSpacing.space8),
        )
    }
}

/**
 * DO7's sources: two columns of cards, a ticked one outlined in `primary`,
 * scrolling inside a fixed [SourceGridHeight] so "Connecter n sources" stays
 * just under them however many the server offers. Loading, failure and no
 * match take the same box, so the button doesn't move between states.
 */
@Composable
private fun WideSourceGrid(content: SourcesContent) {
    Box(modifier = Modifier.fillMaxWidth().height(SourceGridHeight)) {
        when (val state = content.state) {
            SourcesUiState.Loading -> SourcesLoading()
            is SourcesUiState.Failed -> Column { SourcesFailed(state) }
            is SourcesUiState.Loaded -> {
                val matches = filterScripts(state.sources, content.query)
                if (matches.isEmpty()) {
                    NoSources(content)
                    return@Box
                }
                LazyVerticalGrid(
                    columns = GridCells.Fixed(2),
                    horizontalArrangement = Arrangement.spacedBy(AppSpacing.space4),
                    verticalArrangement = Arrangement.spacedBy(AppSpacing.space4),
                    modifier = Modifier.fillMaxSize(),
                ) {
                    items(matches, key = { it.id }) { script ->
                        val checked = script.id in content.selected
                        SourceRow(
                            script = script,
                            checked = checked,
                            enabled = content.canToggle(script),
                            onToggle = { content.onToggle(script) },
                            modifier = Modifier
                                .clip(SourceCardShape)
                                .background(AppColors.surface)
                                .border(
                                    width = if (checked) AppDimensions.borderFocus else AppDimensions.borderHairline,
                                    color = if (checked) AppColors.primary else AppColors.border,
                                    shape = SourceCardShape,
                                ),
                        )
                    }
                }
            }
        }
    }
}

/**
 * How tall [WideSourceGrid] is: four rows of 68dp cards and their gaps, plus
 * a peek of a fifth so it reads as scrollable.
 */
private val SourceGridHeight = 340.dp

private val SourceCardShape = RoundedCornerShape(16.dp)

/** The title and what the step is for, the same at every size. */
@Composable
private fun SourcesHeading() {
    Text(
        text = stringResource(Res.string.sources_title),
        style = AppTypography.display,
        color = AppColors.text,
        modifier = Modifier.semantics { heading() },
    )
    Spacer(Modifier.height(AppSpacing.space3))
    Text(
        text = stringResource(Res.string.sources_subtitle),
        style = AppTypography.body,
        color = AppColors.textMuted,
    )
}

/**
 * The plan counter: "Offre Gratuit" / "Limite atteinte" on phones, "Votre
 * offre Gratuit inclut 5 sources" / "Limite de l'offre Gratuit atteinte"
 * when [wide], the plan's name in SemiBold.
 */
@Composable
private fun SourcesQuota(quota: SourceQuota, selected: Int, wide: Boolean) {
    val plan = planName(quota.planId)
    val used = quota.used(selected)
    val label = when {
        used >= quota.max && wide -> AnnotatedString(stringResource(Res.string.plan_quota_wide_full, plan))
        used >= quota.max -> AnnotatedString(stringResource(Res.string.plan_quota_short_full))
        wide -> withPlanEmphasized(pluralStringResource(Res.plurals.plan_quota_wide, quota.max, plan, quota.max), plan)
        else -> withPlanEmphasized(stringResource(Res.string.plan_quota_short, plan), plan)
    }
    AppPlanQuota(
        label = label,
        used = used,
        max = quota.max,
        description = stringResource(Res.string.plan_quota_description, used, quota.max),
    )
}

/** [text] with its first [plan] in SemiBold, as the mockups set the plan's name. */
private fun withPlanEmphasized(text: String, plan: String): AnnotatedString = buildAnnotatedString {
    val start = text.indexOf(plan)
    if (start < 0) {
        append(text)
        return@buildAnnotatedString
    }
    append(text.substring(0, start))
    withStyle(SpanStyle(fontWeight = FontWeight.SemiBold)) { append(plan) }
    append(text.substring(start + plan.length))
}

@Composable
private fun SourcesSearch(content: SourcesContent) {
    AppSearchField(
        value = content.query,
        onValueChange = content.onQueryChange,
        placeholder = stringResource(Res.string.sources_search_placeholder),
        enabled = content.state is SourcesUiState.Loaded,
        onClear = { content.onQueryChange("") },
        clearContentDescription = stringResource(Res.string.sources_search_clear),
        leadingIcon = { SearchGlyph(modifier = Modifier.size(AppIconSize.sm)) },
    )
}

/**
 * One source: its icon, name and what it fetches, and its checkbox. The
 * whole row toggles it and reads out as one checkbox; when it can't take a
 * tick (the plan is full) it dims to 45% and stays disabled.
 */
@Composable
private fun SourceRow(
    script: FydeScript,
    checked: Boolean,
    enabled: Boolean,
    onToggle: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = modifier
            .toggleable(value = checked, enabled = enabled, role = Role.Checkbox, onValueChange = { onToggle() })
            .alpha(if (enabled) 1f else DISABLED_SOURCE_ALPHA)
            // Inside the toggle, so its hover and press cover the whole card.
            .padding(horizontal = AppSpacing.space5, vertical = AppSpacing.space4),
    ) {
        ScriptIcon(icon = script.icon)
        Spacer(Modifier.width(AppSpacing.space4))
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = script.name,
                style = AppTypography.bodyEmphasis.copy(fontWeight = FontWeight.SemiBold),
                color = AppColors.text,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
            Text(
                text = script.shortDescription.ifBlank { script.description },
                style = AppTypography.body,
                color = AppColors.textMuted,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
        }
        Spacer(Modifier.width(AppSpacing.space3))
        // The row carries the toggle and its semantics, and is the touch
        // target: the box only shows it, without the 48dp it'd reserve.
        CompositionLocalProvider(LocalMinimumInteractiveComponentSize provides 0.dp) {
            AppCheckbox(
                checked = checked,
                onCheckedChange = { onToggle() },
                enabled = enabled,
                modifier = Modifier.clearAndSetSemantics {},
            )
        }
    }
}

/** How much an unticked source fades once the plan is full (the design system's 45%). */
private const val DISABLED_SOURCE_ALPHA = 0.45f

@Composable
private fun SourcesLoading() {
    Box(
        contentAlignment = Alignment.Center,
        modifier = Modifier.fillMaxWidth().padding(vertical = AppSpacing.space9),
    ) {
        CircularProgressIndicator(modifier = Modifier.size(AppIconSize.lg), strokeWidth = 2.dp, color = AppColors.primary)
    }
}

@Composable
private fun SourcesFailed(state: SourcesUiState.Failed) {
    AppNote(
        message = stringResource(Res.string.sources_load_failed, state.message),
        kind = AppNoteKind.Error,
        icon = { tint -> AlertGlyph(modifier = Modifier.size(AppIconSize.sm), tint = tint) },
    )
    Spacer(Modifier.height(AppSpacing.space4))
    AppButton(text = stringResource(Res.string.retry), onClick = state.onRetry, variant = AppButtonVariant.Outlined)
}

/** No source matches the search, or the server offers none at all. */
@Composable
private fun NoSources(content: SourcesContent) {
    Text(
        text = if (content.query.isBlank()) {
            stringResource(Res.string.sources_empty)
        } else {
            stringResource(Res.string.sources_no_match, content.query.trim())
        },
        style = AppTypography.body,
        color = AppColors.textMuted,
        modifier = Modifier.padding(vertical = AppSpacing.space6),
    )
}

/** "Connecter n sources", disabled until a source is ticked ("Passer" is the way on without any). */
@Composable
private fun ConnectButton(content: SourcesContent, onConnect: () -> Unit) {
    val count = content.selected.size
    AppButton(
        text = if (count == 0) {
            stringResource(Res.string.sources_connect_none)
        } else {
            pluralStringResource(Res.plurals.sources_connect, count, count)
        },
        onClick = onConnect,
        enabled = count > 0,
        modifier = Modifier.fillMaxWidth(),
    )
}
