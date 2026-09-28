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
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.sin

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
fun FolderGlyph(modifier: Modifier = Modifier, tint: Color = AppColors.textMuted, strokeWidth: Dp = 1.4.dp) {
    Canvas(modifier = modifier) {
        val stroke = Stroke(width = strokeWidth.toPx(), join = StrokeJoin.Round, cap = StrokeCap.Round)
        val tabHeight = size.height * 0.16f
        val path = Path().apply {
            moveTo(0f, tabHeight)
            lineTo(size.width * 0.38f, tabHeight)
            lineTo(size.width * 0.46f, 0f)
            lineTo(size.width * 0.82f, 0f)
            lineTo(size.width * 0.82f, tabHeight * 0.6f)
            lineTo(size.width, tabHeight * 0.6f)
            lineTo(size.width, size.height)
            lineTo(0f, size.height)
            close()
        }
        drawPath(path, color = tint, style = stroke)
    }
}

@Composable
fun TagGlyph(modifier: Modifier = Modifier, tint: Color = AppColors.textMuted, strokeWidth: Dp = 1.4.dp) {
    Canvas(modifier = modifier) {
        val stroke = Stroke(width = strokeWidth.toPx(), join = StrokeJoin.Round, cap = StrokeCap.Round)
        val path = Path().apply {
            moveTo(0f, size.height * 0.4f)
            lineTo(size.width * 0.58f, 0f)
            lineTo(size.width, 0f)
            lineTo(size.width, size.height * 0.42f)
            lineTo(size.width * 0.42f, size.height)
            close()
        }
        drawPath(path, color = tint, style = stroke)
        drawCircle(color = tint, radius = size.minDimension * 0.08f, center = Offset(size.width * 0.72f, size.height * 0.22f))
    }
}

@Composable
fun BuildingGlyph(modifier: Modifier = Modifier, tint: Color = AppColors.textMuted, strokeWidth: Dp = 1.4.dp) {
    Canvas(modifier = modifier) {
        val stroke = Stroke(width = strokeWidth.toPx(), join = StrokeJoin.Round)
        val bodyLeft = size.width * 0.12f
        val bodyRight = size.width * 0.88f
        drawRect(
            color = tint,
            topLeft = Offset(bodyLeft, size.height * 0.08f),
            size = androidx.compose.ui.geometry.Size(bodyRight - bodyLeft, size.height * 0.92f),
            style = stroke,
        )
        val cols = listOf(0.30f, 0.50f, 0.70f)
        val rows = listOf(0.28f, 0.52f, 0.76f)
        for (r in rows) {
            for (c in cols) {
                drawCircle(color = tint, radius = size.minDimension * 0.035f, center = Offset(size.width * c, size.height * r))
            }
        }
    }
}

@Composable
fun PersonGlyph(modifier: Modifier = Modifier, tint: Color = AppColors.textMuted, strokeWidth: Dp = 1.4.dp) {
    Canvas(modifier = modifier) {
        val stroke = Stroke(width = strokeWidth.toPx(), cap = StrokeCap.Round, join = StrokeJoin.Round)
        val headRadius = size.minDimension * 0.2f
        drawCircle(color = tint, radius = headRadius, center = Offset(size.width / 2f, size.height * 0.28f), style = stroke)
        val path = Path().apply {
            val shoulderTop = size.height * 0.56f
            moveTo(size.width * 0.08f, size.height * 0.96f)
            cubicTo(
                size.width * 0.08f, shoulderTop,
                size.width * 0.92f, shoulderTop,
                size.width * 0.92f, size.height * 0.96f,
            )
        }
        drawPath(path, color = tint, style = stroke)
    }
}

@Composable
fun CalendarGlyph(modifier: Modifier = Modifier, tint: Color = AppColors.textMuted, strokeWidth: Dp = 1.4.dp) {
    Canvas(modifier = modifier) {
        val stroke = Stroke(width = strokeWidth.toPx(), join = StrokeJoin.Round, cap = StrokeCap.Round)
        val top = size.height * 0.18f
        drawRect(
            color = tint,
            topLeft = Offset(size.width * 0.06f, top),
            size = androidx.compose.ui.geometry.Size(size.width * 0.88f, size.height * 0.78f),
            style = stroke,
        )
        drawLine(tint, Offset(size.width * 0.06f, size.height * 0.4f), Offset(size.width * 0.94f, size.height * 0.4f), strokeWidth = stroke.width)
        drawLine(tint, Offset(size.width * 0.28f, 0f), Offset(size.width * 0.28f, top * 1.4f), strokeWidth = stroke.width, cap = StrokeCap.Round)
        drawLine(tint, Offset(size.width * 0.72f, 0f), Offset(size.width * 0.72f, top * 1.4f), strokeWidth = stroke.width, cap = StrokeCap.Round)
    }
}

@Composable
fun StarGlyph(modifier: Modifier = Modifier, tint: Color = AppColors.textMuted, strokeWidth: Dp = 1.4.dp) {
    Canvas(modifier = modifier) {
        val stroke = Stroke(width = strokeWidth.toPx(), join = StrokeJoin.Round, cap = StrokeCap.Round)
        val cx = size.width / 2f
        val cy = size.height / 2f
        val outerR = size.minDimension / 2f
        val innerR = outerR * 0.42f
        val path = Path()
        for (i in 0 until 10) {
            val angle = -PI / 2 + i * PI / 5
            val r = if (i % 2 == 0) outerR else innerR
            val x = cx + (r * cos(angle)).toFloat()
            val y = cy + (r * sin(angle)).toFloat()
            if (i == 0) path.moveTo(x, y) else path.lineTo(x, y)
        }
        path.close()
        drawPath(path, color = tint, style = stroke)
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
