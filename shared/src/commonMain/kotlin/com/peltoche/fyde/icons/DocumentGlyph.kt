package com.peltoche.fyde.icons

import androidx.compose.foundation.Canvas
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.peltoche.fyde.theme.AppColors

/**
 * The folded-corner document glyph used to represent a PDF, drawn without an
 * icon pack (this project has no Material Icons dependency). Shared by
 * `DocumentListScreen`'s per-row file icon, its empty states, and its
 * document-preview pane, so all three read as the same visual vocabulary.
 * Defaults to the theme's own `primary` so it stays correct across themes,
 * rather than a standalone color constant that could silently drift from it.
 */
@Composable
fun DocumentGlyph(modifier: Modifier = Modifier, tint: Color = AppColors.primary, strokeWidth: Dp = 1.4.dp) {
    Canvas(modifier = modifier) {
        val fold = size.minDimension * 0.32f
        val body = Path().apply {
            moveTo(0f, 0f)
            lineTo(size.width - fold, 0f)
            lineTo(size.width, fold)
            lineTo(size.width, size.height)
            lineTo(0f, size.height)
            close()
        }
        drawPath(body, color = tint, style = Stroke(width = strokeWidth.toPx()))

        val flap = Path().apply {
            moveTo(size.width - fold, 0f)
            lineTo(size.width - fold, fold)
            lineTo(size.width, fold)
            close()
        }
        drawPath(flap, color = tint, style = Stroke(width = strokeWidth.toPx() * 0.7f))
    }
}
