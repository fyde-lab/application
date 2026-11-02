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

/** A question a running script is waiting on; completing [answer] hands the reply back to it. */
class PendingQuestion(val text: String, val answer: CompletableDeferred<String>)

/** How a script's last run ended, as [ScraperRuns.outcomes] keeps it. */
sealed interface ScraperRunOutcome {
    /** [found] is how many documents it reported finding, if it said. */
    data class Succeeded(val found: Long?) : ScraperRunOutcome

    /** It failed during [step] (`null` before its first one), [message] saying why in the user's words. */
    data class Failed(val message: String, val step: FydeScraperStep?, val found: Long?) : ScraperRunOutcome
}

/**
 * The one scraper run in progress, if any — what [FydeSdk.runInstalledScript]
 * is doing, as UI state. Owned by `App`, above every screen, with an
 * app-lifetime [scope], so a run started from a script's page keeps going
 * (and can still ask its questions) after the user navigates elsewhere.
 * Found via review: scripts could be installed but never run from the app,
 * so nothing showed whether one ran, how far it got, or that it was waiting
 * on a one-time code.
 *
 * Registered as [FydeSdk]'s [FydeScraperHandler]: progress callbacks update
 * [step]/[update], and a question parks the script on [question] until
 * [answer] is called. A run's end is told in a snackbar, wherever the user
 * is; the Sources tab keeps showing it from [outcomes].
 *
 * Sources just connected or reconfigured (onboarding's [ConnectSourceScreen],
 * the Sources tab) start their run through [runOnceInstalled]: they wait in [queued] for the
 * one in progress, and for their installation to reach this device.
 * [SourceFetchingScreen] (DO7c) follows that run through [stepKind] and
 * [found], then its [outcomes] entry.
 */
@Stable
class ScraperRuns(
    private val scope: CoroutineScope,
    private val snackbarHostState: SnackbarHostState,
) : FydeScraperHandler {
    var running by mutableStateOf<FydeScript?>(null)
        private set
    var step by mutableStateOf<String?>(null)
        private set

    /** Which stage [step] is, for [RunStepper]. */
    var stepKind by mutableStateOf<FydeScraperStep?>(null)
        private set

    /**
     * How many documents the run in progress has found: the total of its
     * last [update] while listing or downloading them, `null` until known.
     */
    var found by mutableStateOf<Long?>(null)
        private set
    var update by mutableStateOf<FydeScraperProgress.Update?>(null)
        private set
    var question by mutableStateOf<PendingQuestion?>(null)
        private set

    private val queue = mutableStateListOf<FydeScript>()

    /** The parameters a queued or starting run waits to see installed, by script id (see [runOnceInstalled]). */
    private val expectedParameters = mutableMapOf<String, Map<String, FydeScriptParameterValue>>()

    private val outcomeByScript = mutableStateMapOf<String, ScraperRunOutcome>()

    /** Each script's last finished run, by script id; dropped as it runs again. */
    val outcomes: Map<String, ScraperRunOutcome> get() = outcomeByScript

    /** Scripts waiting for [running] to finish before their own run, in order. */
    val queued: List<FydeScript> get() = queue

    /** Whether [scriptId] is running or waiting its turn. */
    fun isPending(scriptId: String): Boolean = running?.id == scriptId || queue.any { it.id == scriptId }

    fun run(script: FydeScript) {
        if (running != null) return
        start(script, awaitInstallation = false)
    }

    /**
     * Runs [script], just installed, once its installation has synced to
     * this device ([FydeSdk.installScript] only publishes it) — after the
     * run in progress and those already [queued]. Does nothing if it's
     * already running or queued. Given [expected], it waits for the
     * installation to carry exactly those values, so a reinstall with new
     * credentials doesn't run with the old ones still synced here.
     */
    fun runOnceInstalled(script: FydeScript, expected: Map<String, FydeScriptParameterValue>? = null) {
        if (isPending(script.id)) return
        if (expected != null) expectedParameters[script.id] = expected
        if (running == null) start(script, awaitInstallation = true) else queue += script
    }

    private fun start(script: FydeScript, awaitInstallation: Boolean) {
        running = script
        resetProgress()
        outcomeByScript -= script.id
        scope.launch {
            if (awaitInstallation) awaitInstallation(script.id)
            val result = FydeSdk.runInstalledScript(script.id)
            outcomeByScript[script.id] = result.fold(
                onSuccess = { ScraperRunOutcome.Succeeded(found) },
                onFailure = { ScraperRunOutcome.Failed(it.userMessage(), stepKind, found) },
            )
            running = null
            resetProgress()
            // Before the snackbar, which suspends until it's dismissed.
            queue.removeFirstOrNull()?.let { start(it, awaitInstallation = true) }
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

    private fun resetProgress() {
        step = null
        stepKind = null
        update = null
        found = null
    }

    fun answer(text: String) {
        question?.answer?.complete(text)
    }

    override fun onProgress(progress: FydeScraperProgress) {
        when (progress) {
            is FydeScraperProgress.Step -> {
                step = progress.message
                stepKind = progress.step
                update = null
            }
            is FydeScraperProgress.Update -> {
                update = progress
                val counting = stepKind == FydeScraperStep.LISTING_DOCUMENTS || stepKind == FydeScraperStep.DOWNLOADING_DOCUMENTS
                if (counting && progress.total > 0) found = progress.total
            }
        }
    }

    override suspend fun onQuestion(question: String): String {
        val pending = PendingQuestion(question, CompletableDeferred())
        this.question = pending
        return try {
            pending.answer.await()
        } finally {
            if (this.question === pending) this.question = null
        }
    }
}

/** How often [ScraperRuns.runOnceInstalled] checks whether the installation has synced. */
private const val INSTALL_SYNC_POLL_MS = 500L

/** How long [ScraperRuns.runOnceInstalled] waits for it before running anyway. */
private const val INSTALL_SYNC_TIMEOUT_MS = 30_000L
