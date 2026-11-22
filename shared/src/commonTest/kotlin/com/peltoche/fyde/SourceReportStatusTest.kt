package com.peltoche.fyde

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull

class SourceReportStatusTest {

    private fun report(
        outcome: FydeScraperRunReportOutcome,
        step: FydeScraperStep? = FydeScraperStep.DOWNLOADING_DOCUMENTS,
        update: FydeScraperProgress.Update? = FydeScraperProgress.Update(3, 12),
        found: Long? = 12,
    ) = FydeScraperLastRun(
        installationId = "installation",
        startedAtMs = 1_700_000_000_000,
        endedAtMs = 1_700_000_060_000,
        outcome = outcome,
        step = step,
        update = update,
        found = found,
        saved = 3,
    )

    @Test
    fun aSucceededReportReadsUpToDate() {
        assertEquals(SourceFetchStatus.Succeeded(12), sourceStatusOfReport(report(FydeScraperRunReportOutcome.Succeeded)))
    }

    @Test
    fun aFailedReportKeepsTheSdksWordingAndStep() {
        val status = sourceStatusOfReport(
            report(FydeScraperRunReportOutcome.Failed("Mot de passe refusé"), step = FydeScraperStep.LOGGING_IN, found = null),
        )

        assertEquals(SourceFetchStatus.Failed("Mot de passe refusé", FydeScraperStep.LOGGING_IN, null), status)
        assertEquals(true, status.needsUser())
    }

    @Test
    fun aStoppedReportReadsStopped() {
        assertEquals(
            SourceFetchStatus.Stopped(FydeScraperStep.DOWNLOADING_DOCUMENTS, FydeScraperProgress.Update(3, 12), 12),
            sourceStatusOfReport(report(FydeScraperRunReportOutcome.Stopped)),
        )
    }

    @Test
    fun anInterruptedReportReadsStoppedNeverFailed() {
        val status = sourceStatusOfReport(report(FydeScraperRunReportOutcome.Interrupted))

        assertEquals(
            SourceFetchStatus.Stopped(FydeScraperStep.DOWNLOADING_DOCUMENTS, FydeScraperProgress.Update(3, 12), 12, interrupted = true),
            status,
        )
        assertEquals(false, status.needsUser())
    }

    @Test
    fun aRunningReportLeavesTheStatusToTheLiveRuns() {
        assertNull(sourceStatusOfReport(report(FydeScraperRunReportOutcome.Running)))
    }
}
