package com.peltoche.fyde

import com.peltoche.fyde.components.feedback.RunStepState.Current
import com.peltoche.fyde.components.feedback.RunStepState.Done
import com.peltoche.fyde.components.feedback.RunStepState.Failed
import com.peltoche.fyde.components.feedback.RunStepState.Stopped
import com.peltoche.fyde.components.feedback.RunStepState.ToDo
import kotlin.test.Test
import kotlin.test.assertEquals

class SourceFetchingTest {

    private fun running(step: FydeScraperStep?) = SourceFetchStatus.Running(step, update = null, found = null)

    @Test
    fun startsPreparingUntilTheSdkHasSetTheRunUp() {
        assertEquals(SourceFetchPhase.Preparing, sourceFetchPhaseOf(null))
        assertEquals(SourceFetchPhase.Preparing, sourceFetchPhaseOf(FydeScraperStep.SETTING_UP))
        assertEquals(listOf(Current, ToDo, ToDo, ToDo, ToDo), sourceFetchPhaseStates(running(FydeScraperStep.SETTING_UP)))
    }

    @Test
    fun groupsSignInStepsUnderConnecting() {
        listOf(
            FydeScraperStep.LOGGING_IN,
            FydeScraperStep.CHECKING_SESSION,
            FydeScraperStep.WAITING_FOR_VERIFICATION_CODE,
            FydeScraperStep.WAITING_FOR_CAPTCHA,
        ).forEach { assertEquals(SourceFetchPhase.Connecting, sourceFetchPhaseOf(it)) }
        assertEquals(SourceFetchPhase.Searching, sourceFetchPhaseOf(FydeScraperStep.LISTING_DOCUMENTS))
        assertEquals(SourceFetchPhase.Downloading, sourceFetchPhaseOf(FydeScraperStep.DOWNLOADING_DOCUMENTS))
        assertEquals(SourceFetchPhase.Storing, sourceFetchPhaseOf(FydeScraperStep.DONE))
    }

    @Test
    fun marksEveryPhaseBeforeTheCurrentOneDone() {
        assertEquals(listOf(Current, ToDo, ToDo, ToDo, ToDo), sourceFetchPhaseStates(running(null)))
        assertEquals(listOf(Done, Current, ToDo, ToDo, ToDo), sourceFetchPhaseStates(running(FydeScraperStep.LOGGING_IN)))
        assertEquals(listOf(Done, Done, Done, Current, ToDo), sourceFetchPhaseStates(running(FydeScraperStep.DOWNLOADING_DOCUMENTS)))
        assertEquals(listOf(Done, Done, Done, Done, Current), sourceFetchPhaseStates(running(FydeScraperStep.DONE)))
    }

    @Test
    fun marksThePhaseARunStoppedInFailed() {
        val failed = SourceFetchStatus.Failed("Wrong password", FydeScraperStep.LOGGING_IN, found = null)
        assertEquals(listOf(Done, Failed, ToDo, ToDo, ToDo), sourceFetchPhaseStates(failed))
    }

    @Test
    fun marksThePhaseTheUserStoppedARunInStopped() {
        val stopped = SourceFetchStatus.Stopped(
            FydeScraperStep.DOWNLOADING_DOCUMENTS,
            update = FydeScraperProgress.Update(current = 5, total = 12),
            found = 12,
        )
        assertEquals(listOf(Done, Done, Done, Stopped, ToDo), sourceFetchPhaseStates(stopped))
    }

    @Test
    fun queuedHasNothingDoneAndSucceededHasEverything() {
        assertEquals(List(5) { ToDo }, sourceFetchPhaseStates(SourceFetchStatus.Queued))
        assertEquals(List(5) { Done }, sourceFetchPhaseStates(SourceFetchStatus.Succeeded(found = 12)))
    }
}
