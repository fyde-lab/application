package com.peltoche.fyde

import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.SnackbarResult
import androidx.compose.runtime.Stable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import com.peltoche.fyde.resources.Res
import com.peltoche.fyde.resources.script_run_failed
import com.peltoche.fyde.resources.script_run_finished
import com.peltoche.fyde.resources.script_run_stopped
import com.peltoche.fyde.resources.source_rerun
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withTimeoutOrNull
import org.jetbrains.compose.resources.getString

/**
 * A question a running account is waiting on; completing [answer] hands the
 * reply back to that run alone. [sourceName] names the account ("Ameli ·
 * Marie" when its source has several).
 */
class PendingQuestion(
    val installationId: String,
    val sourceName: String,
    val text: String,
    val answer: CompletableDeferred<String>,
)

/** How an account's last run ended, as [ScraperRuns.outcomes] keeps it. */
sealed interface ScraperRunOutcome {
    /** [found] is how many documents it reported finding, if it said. */
    data class Succeeded(val found: Long?) : ScraperRunOutcome

    /** It failed during [step] (`null` before its first one), [message] saying why in the user's words. */
    data class Failed(val message: String, val step: FydeScraperStep?, val found: Long?) : ScraperRunOutcome

    /**
     * The user stopped it ([ScraperRuns.stop]) during [step] (`null` if it
     * hadn't started). Not a failure: what it saved stays in the vault.
     */
    data class Stopped(
        val step: FydeScraperStep?,
        val update: FydeScraperProgress.Update?,
        val found: Long?,
    ) : ScraperRunOutcome
}

/** One account's run in progress, as [ScraperRuns.runOf] returns it. */
@Stable
class ScraperRun(val account: SourceAccount) {
    val script: FydeScript get() = account.script

    /** While its installation, just published, hasn't reached this device yet. */
    var awaitingInstallation by mutableStateOf(false)
        internal set

    /** Which stage it's at, for [RunStepper]. */
    var stepKind by mutableStateOf<FydeScraperStep?>(null)
        internal set

    /**
     * How many documents it has found: the total of its last [update] while
     * listing or downloading them, `null` until known.
     */
    var found by mutableStateOf<Long?>(null)
        internal set
    var update by mutableStateOf<FydeScraperProgress.Update?>(null)
        internal set

    /** Once [ScraperRuns.stop] was asked for it, until the run has really ended. */
    var stopping by mutableStateOf(false)
        internal set
}

/**
 * Every scraper run in progress — what [FydeSdk.runInstalledScript] is
 * doing, as UI state. Owned by `App`, above every screen, with an
 * app-lifetime [scope], so a run started from a script's page keeps going
 * (and can still ask its questions) after the user navigates elsewhere.
 * Found via review: scripts could be installed but never run from the app,
 * so nothing showed whether one ran, how far it got, or that it was waiting
 * on a one-time code.
 *
 * Runs go in parallel, one per account (installation): the SDK keeps each
 * isolated from the others, even two accounts of the same source, so
 * starting one never waits for another. Each has its own [ScraperRun] (see
 * [runOf]), which progress callbacks update by installation id.
 * A question parks its run on [question] until [answer] is called for
 * it; when several ask at once they're shown one after the other. A run's
 * end is told in a snackbar, wherever the user is; the Sources tab keeps
 * showing it from [outcomes].
 *
 * Registered as [FydeSdk]'s [FydeScraperHandler]. Sources just connected or
 * reconfigured (onboarding's [ConnectSourceScreen], the Sources tab) start
 * their run through [runOnceInstalled], which first waits for their
 * installation to reach this device. [SourceFetchingScreen] (DO7c) follows
 * that run through its [ScraperRun], then its [outcomes] entry, while the
 * next source's run can already be going.
 *
 * [stop] ends a run before it's over (design-system.md § Source runs,
 * Arrêter): its outcome is then [ScraperRunOutcome.Stopped], not a failure.
 */
@Stable
class ScraperRuns(
    private val scope: CoroutineScope,
    private val snackbarHostState: SnackbarHostState,
) : FydeScraperHandler {
    private val active = mutableStateMapOf<String, ScraperRun>()

    private val questions = mutableStateListOf<PendingQuestion>()


    /** The parameters a starting run waits to see installed, by installation id (see [runOnceInstalled]). */
    private val expectedParameters = mutableMapOf<String, Map<String, FydeScriptParameterValue>>()

    private val outcomeById = mutableStateMapOf<String, ScraperRunOutcome>()

    /** Each account's last finished run, by installation id; dropped as it runs again. */
    val outcomes: Map<String, ScraperRunOutcome> get() = outcomeById

    /** The installation ids of every account running now. */
    val runningIds: Set<String> get() = active.keys

    /** The installation ids of every running account [stop] was asked for, until its run has ended. */
    val stoppingIds: Set<String> get() = active.filterValues { it.stopping }.keys

    /** The oldest question still waiting for an answer, if any, leaving out those of runs being stopped. */
    val question: PendingQuestion? get() = questions.firstOrNull { active[it.installationId]?.stopping != true }

    /** [installationId]'s run in progress, if it's running. */
    fun runOf(installationId: String): ScraperRun? = active[installationId]

    /** Whether [installationId] is running (or waiting for its installation to start). */
    fun isPending(installationId: String): Boolean = installationId in active

    fun run(account: SourceAccount) {
        if (isPending(account.installationId)) return
        start(account, awaitInstallation = false)
    }

    /**
     * Runs [account], just installed, once its installation has synced to
     * this device ([FydeSdk.installScript] only publishes it). Does nothing
     * if it's already running. Given [expected], it waits for the
     * installation to carry exactly those values, so a reinstall with new
     * credentials doesn't run with the old ones still synced here.
     */
    fun runOnceInstalled(account: SourceAccount, expected: Map<String, FydeScriptParameterValue>? = null) {
        if (isPending(account.installationId)) return
        if (expected != null) expectedParameters[account.installationId] = expected
        start(account, awaitInstallation = true)
    }

    private fun start(account: SourceAccount, awaitInstallation: Boolean) {
        val id = account.installationId
        val run = ScraperRun(account)
        active[id] = run
        outcomeById -= id
        scope.launch {
            if (awaitInstallation) {
                run.awaitingInstallation = true
                awaitInstallation(run)
                run.awaitingInstallation = false
            }
            // Stopped before it ever reached fyde-sdk: nothing to tell it.
            val result = if (run.stopping) null else FydeSdk.runInstalledScript(id)
            val failure = result?.exceptionOrNull()
            val outcome = when {
                result == null || (failure as? FydeSdkException)?.isRunStopped == true ->
                    ScraperRunOutcome.Stopped(run.stepKind, run.update, run.found)
                failure != null -> ScraperRunOutcome.Failed(failure.userMessage(), run.stepKind, run.found)
                else -> ScraperRunOutcome.Succeeded(run.found)
            }
            outcomeById[id] = outcome
            dismissQuestions(id)
            active -= id
            when (outcome) {
                is ScraperRunOutcome.Succeeded ->
                    snackbarHostState.showSnackbar(getString(Res.string.script_run_finished, account.name))
                is ScraperRunOutcome.Failed ->
                    snackbarHostState.showSnackbar(getString(Res.string.script_run_failed, account.name, outcome.message))
                is ScraperRunOutcome.Stopped -> {
                    val shown = snackbarHostState.showSnackbar(
                        message = getString(Res.string.script_run_stopped, account.name),
                        actionLabel = getString(Res.string.source_rerun),
                    )
                    if (shown == SnackbarResult.ActionPerformed) this@ScraperRuns.run(account)
                }
            }
        }
    }

    /**
     * Stops [installationId]'s run, if it's going (design-system.md § Source
     * runs, Arrêter). Returns at once; the run stays in [runningIds], with
     * [ScraperRun.stopping] set, until it has really ended, then its outcome
     * is [ScraperRunOutcome.Stopped]. Its question, if it was waiting on one,
     * is hidden at once but only answered once the run has ended: an empty
     * answer reaching the script before the stop would fail it as MFA_FAILED.
     */
    fun stop(installationId: String) {
        val run = active[installationId]
        if (run == null) {
            // A question fyde-sdk was still handing over as its run ended.
            dismissQuestions(installationId)
            return
        }
        if (run.stopping) return
        run.stopping = true
        if (run.awaitingInstallation) return // `start` sees `stopping` once the wait ends.
        scope.launch {
            // Between leaving `awaitInstallation` and fyde-sdk registering the
            // run (it first loads the installation), there's nothing to stop
            // yet: try again until the run is over.
            while (active[installationId] === run) {
                if (FydeSdk.stopScriptRun(installationId).getOrDefault(false)) break
                delay(STOP_RETRY_MS)
            }
        }
    }

    /** Answers [installationId]'s pending questions with nothing; fyde-sdk drops an ended run's answers. */
    private fun dismissQuestions(installationId: String) {
        questions.filter { it.installationId == installationId }.forEach { it.answer.complete("") }
    }

    /**
     * Waits until [run]'s installation shows among this device's, or
     * [INSTALL_SYNC_TIMEOUT_MS] passes — the run then fails with the SDK's
     * own wording rather than hanging — or the run is stopped.
     */
    private suspend fun awaitInstallation(run: ScraperRun) {
        val id = run.account.installationId
        val expected = expectedParameters.remove(id)
        withTimeoutOrNull(INSTALL_SYNC_TIMEOUT_MS) {
            while (!run.stopping) {
                val installed = FydeSdk.listInstallations().getOrNull()?.find { it.id == id }
                if (installed != null && (expected == null || installed.parameters == expected)) break
                delay(INSTALL_SYNC_POLL_MS)
            }
        }
    }

    /**
     * Answers [pending], for its account's run only. Takes the question
     * itself, not whichever is first: another can take its place between
     * the dialog showing it and the user sending their answer.
     */
    fun answer(pending: PendingQuestion, text: String) {
        pending.answer.complete(text)
    }

    override fun onProgress(installationId: String, progress: FydeScraperProgress) {
        val run = active[installationId] ?: return
        when (progress) {
            is FydeScraperProgress.Step -> {
                run.stepKind = progress.step
                run.update = null
            }
            is FydeScraperProgress.Update -> {
                run.update = progress
                val counting = run.stepKind == FydeScraperStep.LISTING_DOCUMENTS || run.stepKind == FydeScraperStep.DOWNLOADING_DOCUMENTS
                if (counting && progress.total > 0) run.found = progress.total
            }
        }
    }

    override suspend fun onQuestion(installationId: String, question: String): String {
        val name = active[installationId]?.account?.name.orEmpty()
        val pending = PendingQuestion(installationId, name, question, CompletableDeferred())
        questions += pending
        return try {
            pending.answer.await()
        } finally {
            questions.remove(pending)
        }
    }
}

/**
 * The status [run], an account's last report ([FydeSdk.lastScriptRuns]),
 * shows when this app session hasn't run it: a run cut short by the app
 * closing reads as stopped (muted, "Interrompue"), never as a failure. `null`
 * for one still running: live runs are [ScraperRuns]'.
 */
internal fun sourceStatusOfReport(run: FydeScraperLastRun): SourceFetchStatus? = when (val outcome = run.outcome) {
    FydeScraperRunReportOutcome.Running -> null
    FydeScraperRunReportOutcome.Succeeded -> SourceFetchStatus.Succeeded(run.found)
    is FydeScraperRunReportOutcome.Failed -> SourceFetchStatus.Failed(outcome.message, run.step, run.found)
    FydeScraperRunReportOutcome.Stopped -> SourceFetchStatus.Stopped(run.step, run.update, run.found)
    FydeScraperRunReportOutcome.Interrupted -> SourceFetchStatus.Stopped(run.step, run.update, run.found, interrupted = true)
}

/** How often [ScraperRuns.runOnceInstalled] checks whether the installation has synced. */
private const val INSTALL_SYNC_POLL_MS = 500L

/** How long [ScraperRuns.runOnceInstalled] waits for it before running anyway. */
private const val INSTALL_SYNC_TIMEOUT_MS = 30_000L

/** How often [ScraperRuns.stop] asks again while the run hasn't reached fyde-sdk yet. */
private const val STOP_RETRY_MS = 200L
