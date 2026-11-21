package com.peltoche.fyde

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.ImageBitmap
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/**
 * Reads a document's first page as stored in the SDK's SQLite database
 * (`FydeSdk.documentThumbnail`, which `App.kt` supplies): `null` when none is
 * stored. The SDK never draws one on request, only its background job does
 * (`FfiBackgroundJobs.thumbnails`), and nothing is cached here: each box
 * reads it back. Only where [FydeSdk.showsThumbnails] (desktop): elsewhere
 * there's no reader, so no box ever asks.
 */
internal fun interface DocumentThumbnailReader {
    suspend fun read(documentId: String): Result<FydeThumbnail?>
}

/** The vault's [DocumentThumbnailReader]; `null` outside it (the design-system screen) and off desktop, where nothing has one. */
internal val LocalDocumentThumbnails = staticCompositionLocalOf<DocumentThumbnailReader?> { null }

/**
 * [document]'s first page (the caller scales it to its box), or `null` while
 * it's read, for a document that isn't a PDF, one whose page isn't stored, or
 * one the SDK couldn't draw — the caller then shows its folder's tile.
 */
@Composable
internal fun rememberDocumentThumbnail(document: FydeDocument): ImageBitmap? {
    val reader = LocalDocumentThumbnails.current
    // The size keeps a replaced file from showing its old page.
    val key = if (reader != null && document.kind == DocumentKind.PDF) document.id to document.size else null
    // Keyed, so a row moving on to another document never shows the last one's page meanwhile.
    val image = remember(key, reader) { mutableStateOf<ImageBitmap?>(null) }
    LaunchedEffect(key, reader) {
        if (key == null || reader == null) return@LaunchedEffect
        val png = reader.read(key.first).getOrNull()?.png ?: return@LaunchedEffect
        image.value = withContext(Dispatchers.Default) { decodeIconBitmap(png) }
    }
    return image.value
}
