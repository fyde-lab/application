package com.peltoche.fyde

import androidx.compose.ui.graphics.ImageBitmap

/**
 * Decodes an encoded image — [FydeScript.icon] (always PNG), or an image
 * document's content for [DocumentPreview] — into a drawable bitmap, using
 * each platform's own image codec so no image-decoding library needs to be
 * shared across targets — same expect/actual split as [PdfViewer]. Returns
 * `null` if [bytes] is empty (no icon set) or isn't an image format the
 * platform codec reads (PNG and JPEG everywhere).
 */
internal expect fun decodeIconBitmap(bytes: ByteArray): ImageBitmap?
