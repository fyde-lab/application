package com.peltoche.fyde

import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.toComposeImageBitmap
import java.io.ByteArrayInputStream
import javax.imageio.ImageIO

internal actual fun decodeIconBitmap(bytes: ByteArray): ImageBitmap? {
    if (bytes.isEmpty()) return null
    return ImageIO.read(ByteArrayInputStream(bytes))?.toComposeImageBitmap()
}
