package com.peltoche.fyde

import androidx.compose.runtime.Composable

/** iOS can't keep an app out of screenshots, only notice them afterwards. */
actual val blocksScreenshots: Boolean = false

@Composable
actual fun ScreenshotGuard() = Unit

/** Not wired to `UIPrintInteractionController` yet: the saved PDF prints from the share sheet. */
@Composable
actual fun rememberPdfPrinter(onFailed: () -> Unit): ((jobName: String, pdf: ByteArray) -> Unit)? = null
