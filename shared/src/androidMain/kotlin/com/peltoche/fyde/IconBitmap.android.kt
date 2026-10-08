package com.peltoche.fyde

import android.graphics.BitmapFactory
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asImageBitmap

internal actual fun decodeIconBitmap(bytes: ByteArray): ImageBitmap? {
    if (bytes.isEmpty()) return null
    return BitmapFactory.decodeByteArray(bytes, 0, bytes.size)?.asImageBitmap()
}
