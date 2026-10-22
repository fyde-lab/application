package com.peltoche.fyde

import androidx.compose.runtime.Composable

actual val blocksScreenshots: Boolean = false

@Composable
actual fun ScreenshotGuard() = Unit

/** `fyde-sdk` isn't linked into the Web target (see `FydeSdk.web.kt`), so there's never a kit to print. */
@Composable
actual fun rememberPdfPrinter(onFailed: () -> Unit): ((jobName: String, pdf: ByteArray) -> Unit)? = null
