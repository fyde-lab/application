package com.peltoche.fyde

import android.app.Activity
import android.content.Context
import android.content.ContextWrapper
import android.os.Bundle
import android.os.CancellationSignal
import android.os.ParcelFileDescriptor
import android.print.PageRange
import android.print.PrintAttributes
import android.print.PrintDocumentAdapter
import android.print.PrintDocumentInfo
import android.print.PrintManager
import android.view.WindowManager
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.ui.platform.LocalContext
import java.io.FileOutputStream

actual val blocksScreenshots: Boolean = true

@Composable
actual fun ScreenshotGuard() {
    val activity = LocalContext.current.findActivity()
    DisposableEffect(activity) {
        val window = activity?.window
        window?.addFlags(WindowManager.LayoutParams.FLAG_SECURE)
        onDispose { window?.clearFlags(WindowManager.LayoutParams.FLAG_SECURE) }
    }
}

@Composable
actual fun rememberPdfPrinter(onFailed: () -> Unit): ((jobName: String, pdf: ByteArray) -> Unit)? {
    val latestOnFailed by rememberUpdatedState(onFailed)
    // PrintManager needs an Activity context to show its dialog.
    val activity = LocalContext.current.findActivity() ?: return null
    return remember(activity) {
        { jobName, pdf ->
            // A failed write is reported in the print framework's own UI.
            runCatching {
                val printManager = activity.getSystemService(Context.PRINT_SERVICE) as PrintManager
                printManager.print(jobName, PdfPrintAdapter(jobName, pdf), null)
            }.onFailure { latestOnFailed() }
        }
    }
}

/** Hands an already-laid-out PDF to the print framework as is. */
private class PdfPrintAdapter(private val name: String, private val pdf: ByteArray) : PrintDocumentAdapter() {
    override fun onLayout(
        oldAttributes: PrintAttributes?,
        newAttributes: PrintAttributes,
        cancellationSignal: CancellationSignal,
        callback: LayoutResultCallback,
        extras: Bundle?,
    ) {
        if (cancellationSignal.isCanceled) {
            callback.onLayoutCancelled()
            return
        }
        val info = PrintDocumentInfo.Builder(name)
            .setContentType(PrintDocumentInfo.CONTENT_TYPE_DOCUMENT)
            .setPageCount(1)
            .build()
        callback.onLayoutFinished(info, oldAttributes != newAttributes)
    }

    override fun onWrite(
        pages: Array<out PageRange>,
        destination: ParcelFileDescriptor,
        cancellationSignal: CancellationSignal,
        callback: WriteResultCallback,
    ) {
        runCatching { FileOutputStream(destination.fileDescriptor).use { it.write(pdf) } }
            .onSuccess { callback.onWriteFinished(arrayOf(PageRange.ALL_PAGES)) }
            .onFailure { callback.onWriteFailed(it.message) }
    }
}

private tailrec fun Context.findActivity(): Activity? = when (this) {
    is Activity -> this
    is ContextWrapper -> baseContext.findActivity()
    else -> null
}
