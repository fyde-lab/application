package com.peltoche.fyde

import androidx.compose.runtime.Composable

/** Whether [ScreenshotGuard] keeps the screen out of screenshots on this platform: Android only. */
expect val blocksScreenshots: Boolean

/**
 * While composed, keeps the window out of screenshots, screen recordings and
 * the recent-apps thumbnail where the platform allows it ([blocksScreenshots]),
 * for the recovery key's words; does nothing elsewhere.
 */
@Composable
expect fun ScreenshotGuard()

/**
 * Returns a function handing a PDF to the system's print dialog under a job
 * name, or `null` where the app can't print (iOS and Web, which can still
 * save the PDF and print it from there). [onFailed] runs, on the UI thread,
 * if the PDF never reached the dialog or the printer; cancelling the dialog
 * isn't a failure.
 */
@Composable
expect fun rememberPdfPrinter(onFailed: () -> Unit): ((jobName: String, pdf: ByteArray) -> Unit)?
