package com.peltoche.fyde

import android.graphics.Bitmap
import android.graphics.Color
import android.graphics.pdf.PdfRenderer
import android.os.ParcelFileDescriptor
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.platform.LocalContext
import com.peltoche.fyde.components.feedback.AppLoading
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import java.io.File

/** Bitmap pages are rendered at this multiple of the PDF's own point size (72dpi) for a sharp display. */
private const val RENDER_SCALE = 2

/** An open [PdfRenderer] plus what it reads from, released together by [close]. */
private class OpenPdf(private val file: File, private val descriptor: ParcelFileDescriptor, val renderer: PdfRenderer) {
    fun close() {
        renderer.close()
        descriptor.close()
        file.delete()
    }
}

private sealed interface PdfState {
    data object Loading : PdfState
    data object Failed : PdfState
    class Opened(val pdf: OpenPdf) : PdfState
}

@Composable
actual fun PdfViewer(pdfBytes: ByteArray, modifier: Modifier) {
    val context = LocalContext.current
    // PdfRenderer reads from a file descriptor, so the bytes go to a cache
    // file kept open while the document is on screen — each page is then
    // rendered on demand — and removed as soon as it leaves composition.
    val state by produceState<PdfState>(PdfState.Loading, pdfBytes) {
        val pdf = withContext(Dispatchers.IO) { runCatching { open(context.cacheDir, pdfBytes) }.getOrNull() }
        value = pdf?.let(PdfState::Opened) ?: PdfState.Failed
        awaitDispose { pdf?.close() }
    }

    when (val current = state) {
        PdfState.Loading -> AppLoading(modifier = modifier)
        PdfState.Failed -> PdfRenderFailed(modifier = modifier)
        is PdfState.Opened -> {
            // PdfRenderer allows only one open page at a time.
            val renderLock = remember(current) { Mutex() }
            LazyColumn(modifier = modifier.fillMaxSize()) {
                items(current.pdf.renderer.pageCount) { index ->
                    val page by produceState<ImageBitmap?>(null, current, index) {
                        value = withContext(Dispatchers.IO) {
                            renderLock.withLock { runCatching { render(current.pdf.renderer, index) }.getOrNull() }
                        }
                    }
                    PdfPage(page)
                }
            }
        }
    }
}

private fun open(cacheDir: File, pdfBytes: ByteArray): OpenPdf {
    val file = File.createTempFile("fyde-doc-", ".pdf", cacheDir)
    try {
        file.writeBytes(pdfBytes)
        val descriptor = ParcelFileDescriptor.open(file, ParcelFileDescriptor.MODE_READ_ONLY)
        try {
            return OpenPdf(file, descriptor, PdfRenderer(descriptor))
        } catch (e: Throwable) {
            descriptor.close()
            throw e
        }
    } catch (e: Throwable) {
        file.delete()
        throw e
    }
}

private fun render(renderer: PdfRenderer, index: Int): ImageBitmap =
    renderer.openPage(index).use { page ->
        val bitmap = Bitmap.createBitmap(page.width * RENDER_SCALE, page.height * RENDER_SCALE, Bitmap.Config.ARGB_8888)
        bitmap.eraseColor(Color.WHITE)
        page.render(bitmap, null, null, PdfRenderer.Page.RENDER_MODE_FOR_DISPLAY)
        bitmap.asImageBitmap()
    }
