package com.peltoche.fyde

import android.graphics.Bitmap
import android.graphics.Color
import android.graphics.pdf.PdfRenderer
import android.os.ParcelFileDescriptor
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
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import com.peltoche.fyde.resources.Res
import com.peltoche.fyde.resources.pdf_render_failed
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.jetbrains.compose.resources.stringResource
import java.io.File

/** Bitmap pages are rendered at this multiple of the PDF's own point size (72dpi) for a sharp display. */
private const val RENDER_SCALE = 2

@Composable
actual fun PdfViewer(pdfBytes: ByteArray, modifier: Modifier) {
    val context = LocalContext.current
    var pages by remember(pdfBytes) { mutableStateOf<List<ImageBitmap>?>(null) }
    var error by remember(pdfBytes) { mutableStateOf<String?>(null) }

    LaunchedEffect(pdfBytes) {
        pages = null
        error = null
        withContext(Dispatchers.IO) {
            runCatching { renderPages(context.cacheDir, pdfBytes) }
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

private fun renderPages(cacheDir: File, pdfBytes: ByteArray): List<ImageBitmap> {
    val tempFile = File.createTempFile("fyde-doc-", ".pdf", cacheDir)
    try {
        tempFile.writeBytes(pdfBytes)
        ParcelFileDescriptor.open(tempFile, ParcelFileDescriptor.MODE_READ_ONLY).use { fd ->
            PdfRenderer(fd).use { renderer ->
                return (0 until renderer.pageCount).map { index ->
                    renderer.openPage(index).use { page ->
                        val bitmap = Bitmap.createBitmap(
                            page.width * RENDER_SCALE,
                            page.height * RENDER_SCALE,
                            Bitmap.Config.ARGB_8888,
                        )
                        bitmap.eraseColor(Color.WHITE)
                        page.render(bitmap, null, null, PdfRenderer.Page.RENDER_MODE_FOR_DISPLAY)
                        bitmap.asImageBitmap()
                    }
                }
            }
        }
    } finally {
        tempFile.delete()
    }
}
