package com.peltoche.fyde

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeContentPadding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.em
import com.peltoche.fyde.components.action.AppButton
import com.peltoche.fyde.components.action.AppTextButton
import com.peltoche.fyde.components.content.AppCard
import com.peltoche.fyde.components.feedback.AppNote
import com.peltoche.fyde.components.feedback.AppNoteKind
import com.peltoche.fyde.components.feedback.RunStep
import com.peltoche.fyde.components.feedback.RunStepState
import com.peltoche.fyde.components.feedback.RunStepper
import com.peltoche.fyde.icons.AlertGlyph
import com.peltoche.fyde.icons.InfoGlyph
import com.peltoche.fyde.layout.AppWindowSizeClass
import com.peltoche.fyde.layout.CenteredScrollColumn
import com.peltoche.fyde.layout.LocalAppWindowSizeClass
import com.peltoche.fyde.resources.Res
import com.peltoche.fyde.resources.connect_source_position
import com.peltoche.fyde.resources.source_fetch_background
import com.peltoche.fyde.resources.source_fetch_continue
import com.peltoche.fyde.resources.source_fetch_count
import com.peltoche.fyde.resources.source_fetch_fetched
import com.peltoche.fyde.resources.source_fetch_finished
import com.peltoche.fyde.resources.source_fetch_first
import com.peltoche.fyde.resources.source_fetch_found
import com.peltoche.fyde.resources.source_fetch_next
import com.peltoche.fyde.resources.source_fetch_queued
import com.peltoche.fyde.resources.source_fetch_retry
import com.peltoche.fyde.resources.source_fetch_step_connecting
import com.peltoche.fyde.resources.source_fetch_step_downloading
import com.peltoche.fyde.resources.source_fetch_step_preparing
import com.peltoche.fyde.resources.source_fetch_step_searching
import com.peltoche.fyde.resources.source_fetch_step_storing
import com.peltoche.fyde.resources.source_fetch_title
import com.peltoche.fyde.resources.source_fetch_title_done
import com.peltoche.fyde.resources.source_fetch_title_failed
import com.peltoche.fyde.resources.source_rerun
import com.peltoche.fyde.resources.source_fetching_stopped_note
import com.peltoche.fyde.resources.source_fetch_title_stopped
import com.peltoche.fyde.resources.source_fetch_wait
import com.peltoche.fyde.resources.source_fetch_waiting_for_you
import com.peltoche.fyde.theme.AppColors
import com.peltoche.fyde.theme.AppDimensions
import com.peltoche.fyde.theme.AppIconSize
import com.peltoche.fyde.theme.AppSpacing
import com.peltoche.fyde.theme.AppTypography
import org.jetbrains.compose.resources.pluralStringResource
import org.jetbrains.compose.resources.stringResource

/** Where a just-connected source's first run is, as [SourceFetchingScreen] shows it. */
sealed interface SourceFetchStatus {
    /** Waiting for its installation, just published, to reach this device before it starts. */
    data object Queued : SourceFetchStatus

    /** Running: [step] is its current stage (`null` before the first), [update] how far that stage has got, [found] the documents it has found. */
    data class Running(
        val step: FydeScraperStep?,
        val update: FydeScraperProgress.Update?,
        val found: Long?,
    ) : SourceFetchStatus

    /** Over; [found] documents, if the script said how many. */
    data class Succeeded(val found: Long?) : SourceFetchStatus

    /** Failed during [step], [message] saying why in the user's words. */
    data class Failed(val message: String, val step: FydeScraperStep?, val found: Long?) : SourceFetchStatus

    /**
     * Stopped by the user during [step], [update] how far that stage had got:
     * not a failure. [interrupted] when it was rather cut short by the app
     * closing, as its report tells (see [sourceStatusOfReport]).
     */
    data class Stopped(
        val step: FydeScraperStep?,
        val update: FydeScraperProgress.Update?,
        val found: Long?,
        val interrupted: Boolean = false,
    ) : SourceFetchStatus
}

/**
 * The four stages DO7c lists, coarser than the SDK's [FydeScraperStep]s,
 * after the SDK's own setup ([Preparing], not in DO7c: loading the on-device
 * model, which can take minutes the first time it downloads).
 */
internal enum class SourceFetchPhase { Preparing, Connecting, Searching, Downloading, Storing }

/**
 * The phase [step] belongs to: before the first step, the run is preparing,
 * as it is while the SDK sets it up; signing in, checking the session and
 * waiting for a code or captcha are all "Connexion".
 */
internal fun sourceFetchPhaseOf(step: FydeScraperStep?): SourceFetchPhase = when (step) {
    null,
    FydeScraperStep.SETTING_UP,
    -> SourceFetchPhase.Preparing
    FydeScraperStep.LOGGING_IN,
    FydeScraperStep.CHECKING_SESSION,
    FydeScraperStep.WAITING_FOR_VERIFICATION_CODE,
    FydeScraperStep.WAITING_FOR_CAPTCHA,
    -> SourceFetchPhase.Connecting
    FydeScraperStep.LISTING_DOCUMENTS -> SourceFetchPhase.Searching
    FydeScraperStep.DOWNLOADING_DOCUMENTS -> SourceFetchPhase.Downloading
    FydeScraperStep.DONE -> SourceFetchPhase.Storing
}

/**
 * Each [SourceFetchPhase]'s state for [status]: those before the current
 * one done (a script may skip one, e.g. download without listing first),
 * the current one current — or failed or stopped, if that's where the run
 * ended — the rest still to do. All done once the run succeeded; all to do while
 * it's queued.
 */
internal fun sourceFetchPhaseStates(status: SourceFetchStatus): List<RunStepState> {
    val phases = SourceFetchPhase.entries
    val (reached, atReached) = when (status) {
        SourceFetchStatus.Queued -> return phases.map { RunStepState.ToDo }
        is SourceFetchStatus.Succeeded -> return phases.map { RunStepState.Done }
        is SourceFetchStatus.Running -> sourceFetchPhaseOf(status.step) to RunStepState.Current
        is SourceFetchStatus.Failed -> sourceFetchPhaseOf(status.step) to RunStepState.Failed
        is SourceFetchStatus.Stopped -> sourceFetchPhaseOf(status.step) to RunStepState.Stopped
    }
    return phases.map {
        when {
            it < reached -> RunStepState.Done
            it == reached -> atReached
            else -> RunStepState.ToDo
        }
    }
}

/**
 * Récupération en cours, the vault-creation step after a source is connected
 * on [ConnectSourceScreen]: its first run, followed live. Pure presentation:
 * `App.kt` maps [ScraperRuns] into [status].
 *
 * DO7c at Medium and Expanded (`design/assets/screens/DO7c-recuperation-en-cours.png`):
 * [VaultCreationSplitLayout] at its sources step, listing [sources], beside
 * "Source 1 of 3", the source's tile and what it's doing, a card with the
 * run's [RunStepper] (Préparation → Connexion → Recherche → Téléchargement
 * i / n → Rangement; Préparation isn't in DO7c, see [SourceFetchPhase]), a note that the run goes on in the background and Fyde will
 * say if a code is asked, then "Wait here" and "Next source" ("Continue"
 * after the last, [isLast]). At Compact the same content scrolls, the
 * buttons pinned at the bottom (S4's stepper inside onboarding).
 *
 * Moving on ([onNext]) doesn't stop the run. [onWait] asks to stay until it
 * ends ([isWaiting] then spins "Wait here": `App.kt` moves on by itself if
 * it succeeds). Once it's over the background note goes; a failure shows
 * the SDK's reason as an error note, and [onRetry] in place of "Wait here".
 *
 * Not drawn from the mockup: the time beside "Connexion" — nothing records
 * when a step ended — and "environ une minute", which nothing can estimate.
 */
@Composable
fun SourceFetchingScreen(
    script: FydeScript,
    position: Int,
    count: Int,
    sources: List<VaultSource>,
    status: SourceFetchStatus,
    isLast: Boolean,
    isWaiting: Boolean,
    onWait: () -> Unit,
    onRetry: () -> Unit,
    onNext: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val content = SourceFetchContent(script, position, count, status, isLast, isWaiting, onWait, onRetry, onNext)
    when (LocalAppWindowSizeClass.current) {
        AppWindowSizeClass.Compact -> CompactSourceFetching(content, modifier)
        AppWindowSizeClass.Medium, AppWindowSizeClass.Expanded -> VaultCreationSplitLayout(
            currentStep = VaultCreationStep.Sources,
            sources = sources,
            modifier = modifier,
        ) { paneModifier ->
            WideSourceFetchingPane(content, paneModifier)
        }
    }
}

/** What both layouts of [SourceFetchingScreen] show. */
private class SourceFetchContent(
    val script: FydeScript,
    val position: Int,
    val count: Int,
    val status: SourceFetchStatus,
    val isLast: Boolean,
    val isWaiting: Boolean,
    val onWait: () -> Unit,
    val onRetry: () -> Unit,
    val onNext: () -> Unit,
)

/** At Compact: the content scrolls when the window is too short, the buttons stay pinned at the bottom. */
@Composable
private fun CompactSourceFetching(content: SourceFetchContent, modifier: Modifier) {
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
            Column(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState())
                    .padding(horizontal = AppSpacing.space6),
            ) {
                Spacer(Modifier.height(AppSpacing.space8))
                SourceFetchBody(content)
                Spacer(Modifier.height(AppSpacing.space6))
            }
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = AppSpacing.space6)
                    .padding(top = AppSpacing.space4, bottom = AppSpacing.space5),
            ) {
                SourceFetchActions(content)
            }
        }
    }
}

/** DO7c's pane: the content and its buttons centred in a 440dp column. */
@Composable
private fun WideSourceFetchingPane(content: SourceFetchContent, modifier: Modifier) {
    Box(modifier = modifier.background(AppColors.background).safeContentPadding()) {
        CenteredScrollColumn(
            modifier = Modifier.fillMaxSize(),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Column(
                modifier = Modifier
                    .widthIn(max = AppDimensions.authFormColumn)
                    .fillMaxWidth()
                    .padding(horizontal = AppSpacing.space7, vertical = AppSpacing.space10),
            ) {
                SourceFetchBody(content)
                Spacer(Modifier.height(AppSpacing.space6))
                SourceFetchActions(content)
            }
        }
    }
}

/** "Source 1 of 3", the source's header, the run's stepper in a card, and the note under it. */
@Composable
private fun SourceFetchBody(content: SourceFetchContent) {
    val script = content.script
    val status = content.status
    Text(
        text = stringResource(Res.string.connect_source_position, content.position, content.count).uppercase(),
        style = AppTypography.caption.copy(fontWeight = FontWeight.Medium, letterSpacing = 0.04.em),
        color = AppColors.textMuted,
    )
    Spacer(Modifier.height(AppSpacing.space4))
    Row(verticalAlignment = Alignment.CenterVertically) {
        ScriptIcon(icon = script.icon, size = AppDimensions.iconTileLarge)
        Spacer(Modifier.width(AppSpacing.space5))
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = stringResource(
                    when (status) {
                        is SourceFetchStatus.Succeeded -> Res.string.source_fetch_title_done
                        is SourceFetchStatus.Failed -> Res.string.source_fetch_title_failed
                        is SourceFetchStatus.Stopped -> Res.string.source_fetch_title_stopped
                        SourceFetchStatus.Queued, is SourceFetchStatus.Running -> Res.string.source_fetch_title
                    },
                    script.name,
                ),
                style = AppTypography.title,
                color = AppColors.text,
                modifier = Modifier.semantics { heading() },
            )
            Text(
                text = when (status) {
                    SourceFetchStatus.Queued -> stringResource(Res.string.source_fetch_queued)
                    is SourceFetchStatus.Succeeded -> status.found?.let {
                        pluralStringResource(Res.plurals.source_fetch_fetched, it.toIntClamped(), it)
                    } ?: stringResource(Res.string.source_fetch_finished)
                    is SourceFetchStatus.Running, is SourceFetchStatus.Failed, is SourceFetchStatus.Stopped ->
                        stringResource(Res.string.source_fetch_first)
                },
                style = AppTypography.body,
                color = AppColors.textMuted,
            )
        }
    }
    Spacer(Modifier.height(AppSpacing.space5))
    AppCard(modifier = Modifier.fillMaxWidth()) {
        RunStepper(steps = sourceFetchSteps(status))
    }
    when (status) {
        SourceFetchStatus.Queued, is SourceFetchStatus.Running -> {
            Spacer(Modifier.height(AppSpacing.space4))
            AppNote(
                message = stringResource(Res.string.source_fetch_background, script.name),
                kind = AppNoteKind.Info,
                icon = { tint -> InfoGlyph(modifier = Modifier.size(AppIconSize.md), tint = tint) },
            )
        }
        is SourceFetchStatus.Failed -> {
            Spacer(Modifier.height(AppSpacing.space4))
            AppNote(
                message = status.message,
                kind = AppNoteKind.Error,
                icon = { tint -> AlertGlyph(modifier = Modifier.size(AppIconSize.sm), tint = tint) },
            )
        }
        is SourceFetchStatus.Stopped -> {
            Spacer(Modifier.height(AppSpacing.space4))
            AppNote(
                message = stringResource(Res.string.source_fetching_stopped_note),
                kind = AppNoteKind.Info,
                icon = { tint -> InfoGlyph(modifier = Modifier.size(AppIconSize.md), tint = tint) },
            )
        }
        is SourceFetchStatus.Succeeded -> Unit
    }
}

/** [status]'s five phases as [RunStepper] rows, with what's known of each: found, i / n, waiting on the user. */
@Composable
internal fun sourceFetchSteps(status: SourceFetchStatus): List<RunStep> {
    val states = sourceFetchPhaseStates(status)
    val running = status as? SourceFetchStatus.Running
    val found = when (status) {
        is SourceFetchStatus.Running -> status.found
        is SourceFetchStatus.Succeeded -> status.found
        is SourceFetchStatus.Failed -> status.found
        is SourceFetchStatus.Stopped -> status.found
        SourceFetchStatus.Queued -> null
    }
    // While downloading, its i / n (where it stood when stopped, too); while
    // listing, how many it has come across so far.
    val update = (running?.update ?: (status as? SourceFetchStatus.Stopped)?.update)?.takeIf { it.total > 0 }
    return SourceFetchPhase.entries.mapIndexed { index, phase ->
        val state = states[index]
        when (phase) {
            SourceFetchPhase.Preparing -> RunStep(
                label = stringResource(Res.string.source_fetch_step_preparing),
                state = state,
            )
            SourceFetchPhase.Connecting -> RunStep(
                label = stringResource(Res.string.source_fetch_step_connecting),
                state = state,
                detail = stringResource(Res.string.source_fetch_waiting_for_you).takeIf {
                    state == RunStepState.Current && running?.step.isWaitingForUser()
                },
            )
            SourceFetchPhase.Searching -> RunStep(
                label = stringResource(Res.string.source_fetch_step_searching),
                state = state,
                detail = found?.takeIf { state != RunStepState.ToDo }?.let {
                    pluralStringResource(Res.plurals.source_fetch_found, it.toIntClamped(), it)
                },
            )
            SourceFetchPhase.Downloading -> {
                val downloading = update?.takeIf { state == RunStepState.Current || state == RunStepState.Stopped }
                RunStep(
                    label = stringResource(Res.string.source_fetch_step_downloading),
                    state = state,
                    detail = downloading?.let { stringResource(Res.string.source_fetch_count, it.current, it.total) },
                    // A stopped download's bar would read as still going.
                    progress = downloading?.takeIf { state == RunStepState.Current }?.let { it.current.toFloat() / it.total },
                )
            }
            SourceFetchPhase.Storing -> RunStep(
                label = stringResource(Res.string.source_fetch_step_storing),
                state = state,
            )
        }
    }
}

internal fun FydeScraperStep?.isWaitingForUser(): Boolean =
    this == FydeScraperStep.WAITING_FOR_VERIFICATION_CODE || this == FydeScraperStep.WAITING_FOR_CAPTCHA

internal fun Long.toIntClamped(): Int = coerceAtMost(Int.MAX_VALUE.toLong()).toInt()

/**
 * "Wait here" (or "Retry" after a failure; nothing once it succeeded) as
 * text, and "Next source" — "Continue" after the last — filled, twice as wide.
 */
@Composable
private fun SourceFetchActions(content: SourceFetchContent) {
    val next = stringResource(if (content.isLast) Res.string.source_fetch_continue else Res.string.source_fetch_next)
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(AppSpacing.space3),
    ) {
        when (content.status) {
            SourceFetchStatus.Queued, is SourceFetchStatus.Running -> AppTextButton(
                text = stringResource(Res.string.source_fetch_wait),
                onClick = content.onWait,
                loading = content.isWaiting,
                enabled = !content.isWaiting,
            )
            is SourceFetchStatus.Failed -> AppTextButton(
                text = stringResource(Res.string.source_fetch_retry),
                onClick = content.onRetry,
            )
            is SourceFetchStatus.Stopped -> AppTextButton(
                text = stringResource(Res.string.source_rerun),
                onClick = content.onRetry,
            )
            is SourceFetchStatus.Succeeded -> Unit
        }
        AppButton(text = next, onClick = content.onNext, modifier = Modifier.weight(1f))
    }
}
