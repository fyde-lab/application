package com.peltoche.fyde

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.interop.UIKitView
import kotlinx.cinterop.ExperimentalForeignApi
import kotlinx.cinterop.allocArrayOf
import kotlinx.cinterop.memScoped
import platform.Foundation.NSData
import platform.Foundation.create
import platform.PDFKit.PDFDisplayMode
import platform.PDFKit.PDFDocument
import platform.PDFKit.PDFView

/** Native PDFKit view, scrolled and zoomed by the system rather than by our own Compose UI. */
@OptIn(ExperimentalForeignApi::class)
@Composable
actual fun PdfViewer(pdfBytes: ByteArray, modifier: Modifier) {
    UIKitView(
        factory = {
            PDFView().apply {
                autoScales = true
                displayMode = PDFDisplayMode.PDFDisplayModeSinglePageContinuous
                document = PDFDocument(data = pdfBytes.toNSData())
            }
        },
        modifier = modifier,
    )
}

@OptIn(ExperimentalForeignApi::class)
private fun ByteArray.toNSData(): NSData = memScoped {
    NSData.create(bytes = allocArrayOf(this@toNSData), length = this@toNSData.size.toULong())
}
