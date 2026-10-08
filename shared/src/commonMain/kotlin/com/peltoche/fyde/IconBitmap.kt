package com.peltoche.fyde

import androidx.compose.ui.graphics.ImageBitmap

/**
 * Decodes [FydeScript.icon] (always PNG-encoded) into a drawable bitmap,
 * using each platform's own image codec so no image-decoding library needs
 * to be shared across targets — same expect/actual split as [PdfViewer].
 * Returns `null` if [bytes] is empty (no icon set) or isn't valid PNG data.
 */
internal expect fun decodeIconBitmap(bytes: ByteArray): ImageBitmap?
