package com.peltoche.fyde

import androidx.compose.material3.SnackbarHostState
import androidx.compose.runtime.Stable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import com.peltoche.fyde.resources.Res
import com.peltoche.fyde.resources.script_run_failed
import com.peltoche.fyde.resources.script_run_finished
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withTimeoutOrNull
import org.jetbrains.compose.resources.getString

/**
 * A question a running script is waiting on; completing [answer] hands the
 * reply back to that script's run alone.
 */
class PendingQuestion(
    val scriptId: String,
    val scriptName: String,
    val text: String,
    val answer: CompletableDeferred<String>,
)

/** How a script's last run ended, as [ScraperRuns.outcomes] keeps it. */
sealed interface ScraperRunOutcome {
    /** [found] is how many documents it reported finding, if it said. */
    data class Succeeded(val found: Long?) : ScraperRunOutcome

    /** It failed during [step] (`null` before its first one), [message] saying why in the user's words. */
    data class Failed(val message: String, val step: FydeScraperStep?, val found: Long?) : ScraperRunOutcome
}

/** One script's run in progress, as [ScraperRuns.runOf] returns it. */
@Stable
class ScraperRun(val script: FydeScript) {
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
 * Runs go in parallel, one per script: the SDK keeps each isolated from
 * the others, so starting one never waits for another. Each has its own
 * [ScraperRun] (see [runOf]), which progress callbacks update by script id.
 * A question parks its script on [question] until [answer] is called for
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
 */
@Stable
class ScraperRuns(
    private val scope: CoroutineScope,
    private val snackbarHostState: SnackbarHostState,
) : FydeScraperHandler {
    private val active = mutableStateMapOf<String, ScraperRun>()

    private val questions = mutableStateListOf<PendingQuestion>()


    /** The parameters a starting run waits to see installed, by script id (see [runOnceInstalled]). */
    private val expectedParameters = mutableMapOf<String, Map<String, FydeScriptParameterValue>>()

    private val outcomeByScript = mutableStateMapOf<String, ScraperRunOutcome>()

    /** Each script's last finished run, by script id; dropped as it runs again. */
    val outcomes: Map<String, ScraperRunOutcome> get() = outcomeByScript

    /** The ids of every script running now. */
    val runningIds: Set<String> get() = active.keys

    /** The oldest question still waiting for an answer, if any. */
    val question: PendingQuestion? get() = questions.firstOrNull()

    /** [scriptId]'s run in progress, if it's running. */
    fun runOf(scriptId: String): ScraperRun? = active[scriptId]

    /** Whether [scriptId] is running (or waiting for its installation to start). */
    fun isPending(scriptId: String): Boolean = scriptId in active

    fun run(script: FydeScript) {
        if (isPending(script.id)) return
        start(script, awaitInstallation = false)
    }

    /**
     * Runs [script], just installed, once its installation has synced to
     * this device ([FydeSdk.installScript] only publishes it). Does nothing
     * if it's already running. Given [expected], it waits for the
     * installation to carry exactly those values, so a reinstall with new
     * credentials doesn't run with the old ones still synced here.
     */
    fun runOnceInstalled(script: FydeScript, expected: Map<String, FydeScriptParameterValue>? = null) {
        if (isPending(script.id)) return
        if (expected != null) expectedParameters[script.id] = expected
        start(script, awaitInstallation = true)
    }

    private fun start(script: FydeScript, awaitInstallation: Boolean) {
        val run = ScraperRun(script)
        active[script.id] = run
        outcomeByScript -= script.id
        scope.launch {
            if (awaitInstallation) {
                run.awaitingInstallation = true
                awaitInstallation(script.id)
                run.awaitingInstallation = false
            }
            val result = FydeSdk.runInstalledScript(script.id)
            outcomeByScript[script.id] = result.fold(
                onSuccess = { ScraperRunOutcome.Succeeded(run.found) },
                onFailure = { ScraperRunOutcome.Failed(it.userMessage(), run.stepKind, run.found) },
            )
            active -= script.id
            result
                .onSuccess { snackbarHostState.showSnackbar(getString(Res.string.script_run_finished, script.name)) }
                .onFailure { snackbarHostState.showSnackbar(getString(Res.string.script_run_failed, script.name, it.userMessage())) }
        }
    }

    /**
     * Waits until [scriptId]'s installation shows among this device's, or
     * [INSTALL_SYNC_TIMEOUT_MS] passes — the run then fails with the SDK's
     * own wording rather than hanging.
     */
    private suspend fun awaitInstallation(scriptId: String) {
        val expected = expectedParameters.remove(scriptId)
        withTimeoutOrNull(INSTALL_SYNC_TIMEOUT_MS) {
            while (true) {
                val installed = FydeSdk.installedScriptParameters().getOrNull()?.get(scriptId)
                if (installed != null && (expected == null || installed == expected)) break
                delay(INSTALL_SYNC_POLL_MS)
            }
        }
    }

    /**
     * Answers [pending], for its script's run only. Takes the question
     * itself, not whichever is first: another can take its place between
     * the dialog showing it and the user sending their answer.
     */
    fun answer(pending: PendingQuestion, text: String) {
        pending.answer.complete(text)
    }

    override fun onProgress(scriptId: String, progress: FydeScraperProgress) {
        val run = active[scriptId] ?: return
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

    override suspend fun onQuestion(scriptId: String, question: String): String {
        val name = active[scriptId]?.script?.name.orEmpty()
        val pending = PendingQuestion(scriptId, name, question, CompletableDeferred())
        questions += pending
        return try {
            pending.answer.await()
        } finally {
            questions.remove(pending)
        }
    }
}

/** How often [ScraperRuns.runOnceInstalled] checks whether the installation has synced. */
private const val INSTALL_SYNC_POLL_MS = 500L

/** How long [ScraperRuns.runOnceInstalled] waits for it before running anyway. */
private const val INSTALL_SYNC_TIMEOUT_MS = 30_000L
