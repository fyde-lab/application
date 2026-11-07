package com.peltoche.fyde.components.content

import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.unit.Dp
import com.peltoche.fyde.icons.PuzzleGlyph
import com.peltoche.fyde.theme.AppColors
import com.peltoche.fyde.theme.AppDimensions
import com.peltoche.fyde.theme.AppRadius

/**
 * A neutral [AppIconTile] showing a third-party [image] (a source's logo),
 * or a drawn [PuzzleGlyph] when there's none. Logos arrive in wildly
 * inconsistent shapes and paddings, so every one is inset the same amount
 * and the tile reads as a uniform frame.
 */
@Composable
fun AppImageTile(image: ImageBitmap?, modifier: Modifier = Modifier, size: Dp = AppDimensions.iconTile) {
    AppIconTile(modifier = modifier, size = size) {
        if (image != null) {
            Image(
                bitmap = image,
                contentDescription = null,
                contentScale = ContentScale.Fit,
                modifier = Modifier.size(size * IMAGE_SCALE).clip(AppRadius.sm),
            )
        } else {
            PuzzleGlyph(tint = AppColors.textMuted, modifier = Modifier.size(size * GLYPH_SCALE))
        }
    }
}

/** How much of the tile a logo fills. */
private const val IMAGE_SCALE = 0.72f

/** How much of the tile the fallback glyph fills. */
private const val GLYPH_SCALE = 0.45f
