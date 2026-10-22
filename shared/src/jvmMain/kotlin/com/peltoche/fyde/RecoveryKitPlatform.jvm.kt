package com.peltoche.fyde

import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.rememberUpdatedState
import java.awt.print.PrinterJob
import java.util.logging.Logger
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.apache.pdfbox.Loader
import org.apache.pdfbox.printing.PDFPageable

private val logger = Logger.getLogger("RecoveryKitPlatform")

/** A desktop OS offers no way to keep a window out of screenshots that Compose Desktop exposes. */
actual val blocksScreenshots: Boolean = false

@Composable
actual fun ScreenshotGuard() = Unit

@Composable
actual fun rememberPdfPrinter(onFailed: () -> Unit): ((jobName: String, pdf: ByteArray) -> Unit)? {
    val latestOnFailed by rememberUpdatedState(onFailed)
    val scope = rememberCoroutineScope()
    return remember {
        { jobName, pdf ->
            // Off the UI thread: the print dialog is modal and printing blocks.
            scope.launch {
                val printed = withContext(Dispatchers.IO) {
                    runCatching {
                        Loader.loadPDF(pdf).use { document ->
                            val job = PrinterJob.getPrinterJob()
                            job.jobName = jobName
                            job.setPageable(PDFPageable(document))
                            if (job.printDialog()) job.print()
                        }
                    }
                }
                printed.onFailure {
                    logger.warning("printing $jobName failed: $it")
                    latestOnFailed()
                }
            }
        }
    }
}
