package com.peltoche.fyde

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.Dp
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.sync.Semaphore
import kotlinx.coroutines.sync.withPermit
import kotlinx.coroutines.withContext

/**
 * Documents' first pages as drawn by the SDK (`FydeSdk.documentThumbnail`,
 * through [render], which `App.kt` supplies), decoded and kept in memory
 * while the vault is shown: the SDK draws one on every call and stores
 * none. Least recently used first out past [budgetBytes] of pixels; a
 * document it couldn't draw is remembered too, so it isn't tried again on
 * every scroll. Never on disk: it goes with the composition holding it,
 * logout included.
 *
 * Only PDFs have one (the SDK draws nothing else); everywhere else rows and
 * cards keep their folder's tile.
 */
internal class DocumentThumbnails(
    private val render: suspend (id: String, maxWidth: Int, maxHeight: Int) -> Result<FydeThumbnail?>,
    private val budgetBytes: Long = THUMBNAIL_CACHE_BYTES,
) {
    /** A drawing at one size: the same document at another box is drawn again, sharp. */
    data class Key(val documentId: String, val size: Long, val maxWidth: Int, val maxHeight: Int)

    // Insertion order is recency order: a hit is moved to the end.
    // Only touched from the composition's (main) thread.
    private val entries = LinkedHashMap<Key, ImageBitmap?>()
    private var bytes = 0L
    private val drawing = Semaphore(MAX_CONCURRENT_RENDERS)

    /** Whether [key] has been drawn (or failed to be), and if so its image. */
    fun peek(key: Key): Cached? {
        if (!entries.containsKey(key)) return null
        val image = entries.remove(key)
        entries[key] = image
        return Cached(image)
    }

    /** [key]'s image, drawn by the SDK unless it already was; `null` for none. */
    suspend fun load(key: Key): ImageBitmap? {
        peek(key)?.let { return it.image }
        val result = drawing.withPermit { render(key.documentId, key.maxWidth, key.maxHeight) }
        // Not cached on this device (yet): asked again next time.
        if (result.getOrNull() == null && result.isSuccess) return null
        val image = result.getOrNull()?.let { withContext(Dispatchers.Default) { decodeIconBitmap(it.png) } }
        put(key, image)
        return image
    }

    private fun put(key: Key, image: ImageBitmap?) {
        entries.remove(key)?.let { bytes -= it.byteCount }
        entries[key] = image
        bytes += image.byteCount
        val iterator = entries.entries.iterator()
        while (bytes > budgetBytes && iterator.hasNext()) {
            val eldest = iterator.next()
            if (eldest.key == key) break
            bytes -= eldest.value.byteCount
            iterator.remove()
        }
    }

    class Cached(val image: ImageBitmap?)

    private val ImageBitmap?.byteCount: Long get() = if (this == null) 0L else width.toLong() * height * 4

    companion object {
        /** ~32 MiB of pixels: a few hundred row thumbnails, or a hundred cards at 2×. */
        const val THUMBNAIL_CACHE_BYTES: Long = 32L * 1024 * 1024

        /** Each drawing takes one of the SDK's blocking threads: a screenful of rows shouldn't take them all. */
        private const val MAX_CONCURRENT_RENDERS = 4
    }
}

/** The vault's [DocumentThumbnails]; `null` outside it (the design-system screen), where nothing has one. */
internal val LocalDocumentThumbnails = staticCompositionLocalOf<DocumentThumbnails?> { null }

/**
 * [document]'s first page fitting [width] x [height], or `null` while it's
 * drawn, for a document that isn't a PDF, or one the SDK can't draw — the
 * caller then shows its folder's tile.
 */
@Composable
internal fun rememberDocumentThumbnail(document: FydeDocument, width: Dp, height: Dp): ImageBitmap? {
    val thumbnails = LocalDocumentThumbnails.current
    val density = LocalDensity.current
    val key = if (thumbnails != null && document.kind == DocumentKind.PDF) {
        DocumentThumbnails.Key(
            documentId = document.id,
            size = document.size,
            maxWidth = with(density) { width.roundToPx() },
            maxHeight = with(density) { height.roundToPx() },
        )
    } else {
        null
    }
    // Keyed, so a row moving on to another document never shows the last one's page meanwhile.
    val image = remember(key, thumbnails) { mutableStateOf(key?.let { thumbnails?.peek(it) }?.image) }
    LaunchedEffect(key, thumbnails) {
        if (key != null && thumbnails != null && image.value == null) image.value = thumbnails.load(key)
    }
    return image.value
}
