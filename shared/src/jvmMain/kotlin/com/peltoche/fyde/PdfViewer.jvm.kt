package com.peltoche.fyde

import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.toComposeImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.unit.dp
import com.peltoche.fyde.resources.Res
import com.peltoche.fyde.resources.pdf_render_failed
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.apache.pdfbox.Loader
import org.apache.pdfbox.rendering.PDFRenderer
import org.jetbrains.compose.resources.stringResource

/** Pages are rasterized at this DPI, a middle ground between sharpness and render time/memory. */
private const val RENDER_DPI = 150f

@Composable
actual fun PdfViewer(pdfBytes: ByteArray, modifier: Modifier) {
    var pages by remember(pdfBytes) { mutableStateOf<List<ImageBitmap>?>(null) }
    var error by remember(pdfBytes) { mutableStateOf<String?>(null) }

    LaunchedEffect(pdfBytes) {
        pages = null
        error = null
        withContext(Dispatchers.IO) {
            runCatching { renderPages(pdfBytes) }
        }.onSuccess { pages = it }.onFailure { error = it.message }
    }

    Box(modifier = modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        val currentPages = pages
        val currentError = error
        when {
            currentError != null -> Text(
                text = stringResource(Res.string.pdf_render_failed, currentError),
                color = MaterialTheme.colorScheme.error,
                modifier = Modifier.padding(horizontal = 32.dp),
            )
            currentPages == null -> CircularProgressIndicator(color = MaterialTheme.colorScheme.primary)
            else -> LazyColumn(modifier = Modifier.fillMaxSize()) {
                items(currentPages) { page ->
                    Image(
                        bitmap = page,
                        contentDescription = null,
                        contentScale = ContentScale.FillWidth,
                        modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp),
                    )
                }
            }
        }
    }
}

private fun renderPages(pdfBytes: ByteArray): List<ImageBitmap> =
    Loader.loadPDF(pdfBytes).use { document ->
        val renderer = PDFRenderer(document)
        (0 until document.numberOfPages).map { index ->
            renderer.renderImageWithDPI(index, RENDER_DPI).toComposeImageBitmap()
        }
    }
