package com.peltoche.fyde

import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.toComposeImageBitmap
import com.peltoche.fyde.components.feedback.AppLoading
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import org.apache.pdfbox.Loader
import org.apache.pdfbox.pdmodel.PDDocument
import org.apache.pdfbox.rendering.PDFRenderer

/** Pages are rasterized at this DPI, a middle ground between sharpness and render time/memory. */
private const val RENDER_DPI = 150f

private sealed interface PdfState {
    data object Loading : PdfState
    data object Failed : PdfState
    class Opened(val document: PDDocument) : PdfState
}

@Composable
actual fun PdfViewer(pdfBytes: ByteArray, modifier: Modifier) {
    // The document stays open while it's on screen, so each page can be
    // rendered on demand, and is closed as soon as it leaves composition.
    val state by produceState<PdfState>(PdfState.Loading, pdfBytes) {
        val document = withContext(Dispatchers.IO) { runCatching { Loader.loadPDF(pdfBytes) }.getOrNull() }
        value = document?.let(PdfState::Opened) ?: PdfState.Failed
        awaitDispose { document?.close() }
    }

    when (val current = state) {
        PdfState.Loading -> AppLoading(modifier = modifier)
        PdfState.Failed -> PdfRenderFailed(modifier = modifier)
        is PdfState.Opened -> {
            val renderer = remember(current) { PDFRenderer(current.document) }
            // PDFBox isn't thread-safe, so pages render one at a time.
            val renderLock = remember(current) { Mutex() }
            LazyColumn(modifier = modifier.fillMaxSize()) {
                items(current.document.numberOfPages) { index ->
                    val page by produceState<ImageBitmap?>(null, current, index) {
                        value = withContext(Dispatchers.IO) {
                            renderLock.withLock {
                                runCatching { renderer.renderImageWithDPI(index, RENDER_DPI).toComposeImageBitmap() }.getOrNull()
                            }
                        }
                    }
                    PdfPage(page)
                }
            }
        }
    }
}
