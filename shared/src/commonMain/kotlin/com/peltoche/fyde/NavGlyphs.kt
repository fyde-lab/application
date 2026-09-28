package com.peltoche.fyde

import androidx.compose.foundation.Canvas
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.peltoche.fyde.theme.AppColors

/**
 * Hand-drawn `Canvas` glyphs for the desktop sidebar and list rows, in the
 * same vocabulary as [DocumentGlyph] — this project has no icon pack (see
 * `design/design-system.md`'s open questions), so every icon here is a
 * small geometric shape rather than a font glyph or bitmap asset.
 */

@Composable
fun SearchGlyph(modifier: Modifier = Modifier, tint: Color = AppColors.textMuted, strokeWidth: Dp = 1.6.dp) {
    Canvas(modifier = modifier) {
        val stroke = Stroke(width = strokeWidth.toPx(), cap = StrokeCap.Round)
        val radius = size.minDimension * 0.32f
        val center = Offset(size.width * 0.42f, size.height * 0.42f)
        drawCircle(color = tint, radius = radius, center = center, style = stroke)
        val handleStart = Offset(center.x + radius * 0.74f, center.y + radius * 0.74f)
        drawLine(color = tint, start = handleStart, end = Offset(size.width * 0.92f, size.height * 0.92f), strokeWidth = stroke.width, cap = StrokeCap.Round)
    }
}

@Composable
fun LockGlyph(modifier: Modifier = Modifier, tint: Color = AppColors.success, strokeWidth: Dp = 1.6.dp) {
    Canvas(modifier = modifier) {
        val stroke = Stroke(width = strokeWidth.toPx(), join = StrokeJoin.Round, cap = StrokeCap.Round)
        val bodyTop = size.height * 0.48f
        drawRect(
            color = tint,
            topLeft = Offset(size.width * 0.16f, bodyTop),
            size = androidx.compose.ui.geometry.Size(size.width * 0.68f, size.height * 0.44f),
            style = stroke,
        )
        val shacklePath = Path().apply {
            val shackleWidth = size.width * 0.44f
            val left = (size.width - shackleWidth) / 2f
            moveTo(left, bodyTop)
            arcTo(
                rect = androidx.compose.ui.geometry.Rect(left, size.height * 0.06f, left + shackleWidth, bodyTop + shackleWidth / 2f),
                startAngleDegrees = 180f,
                sweepAngleDegrees = -180f,
                forceMoveTo = false,
            )
            lineTo(left + shackleWidth, bodyTop)
        }
        drawPath(shacklePath, color = tint, style = stroke)
    }
}

@Composable
fun GearGlyph(modifier: Modifier = Modifier, tint: Color = AppColors.textMuted, strokeWidth: Dp = 1.4.dp) {
    Canvas(modifier = modifier) {
        val stroke = Stroke(width = strokeWidth.toPx(), cap = StrokeCap.Round)
        val center = Offset(size.width / 2f, size.height / 2f)
        val innerRadius = size.minDimension * 0.22f
        val toothInner = size.minDimension * 0.32f
        val toothOuter = size.minDimension * 0.48f
        drawCircle(color = tint, radius = innerRadius, center = center, style = stroke)
        repeat(8) { i ->
            rotate(degrees = i * 45f, pivot = center) {
                drawLine(
                    color = tint,
                    start = Offset(center.x, center.y - toothInner),
                    end = Offset(center.x, center.y - toothOuter),
                    strokeWidth = stroke.width,
                    cap = StrokeCap.Round,
                )
            }
        }
    }
}

@Composable
fun CodeGlyph(modifier: Modifier = Modifier, tint: Color = AppColors.textMuted, strokeWidth: Dp = 1.4.dp) {
    Canvas(modifier = modifier) {
        val stroke = Stroke(width = strokeWidth.toPx(), cap = StrokeCap.Round, join = StrokeJoin.Round)
        val leftChevron = Path().apply {
            moveTo(size.width * 0.38f, size.height * 0.18f)
            lineTo(size.width * 0.06f, size.height * 0.5f)
            lineTo(size.width * 0.38f, size.height * 0.82f)
        }
        drawPath(leftChevron, color = tint, style = stroke)
        val rightChevron = Path().apply {
            moveTo(size.width * 0.62f, size.height * 0.18f)
            lineTo(size.width * 0.94f, size.height * 0.5f)
            lineTo(size.width * 0.62f, size.height * 0.82f)
        }
        drawPath(rightChevron, color = tint, style = stroke)
    }
}

@Composable
fun ChevronRightGlyph(modifier: Modifier = Modifier, tint: Color = AppColors.textMuted, strokeWidth: Dp = 1.4.dp) {
    Canvas(modifier = modifier) {
        val stroke = Stroke(width = strokeWidth.toPx(), cap = StrokeCap.Round, join = StrokeJoin.Round)
        val path = Path().apply {
            moveTo(size.width * 0.3f, 0f)
            lineTo(size.width * 0.85f, size.height / 2f)
            lineTo(size.width * 0.3f, size.height)
        }
        drawPath(path, color = tint, style = stroke)
    }
}
