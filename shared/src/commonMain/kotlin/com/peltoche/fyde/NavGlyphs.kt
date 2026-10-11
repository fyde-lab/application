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

/** Two angle brackets, `<` `>` — the sidebar's Scripts destination. */
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

/**
 * A puzzle piece — the sidebar's Addons destination and the addon grid's
 * per-card icon chip. One knob on the right edge and one socket on the
 * bottom edge are enough to read as "puzzle piece" at nav-icon size without
 * drawing all four, keeping it a single closed outline like every other
 * glyph here rather than four small circular bumps.
 */
@Composable
fun PuzzleGlyph(modifier: Modifier = Modifier, tint: Color = AppColors.textMuted, strokeWidth: Dp = 1.6.dp) {
    Canvas(modifier = modifier) {
        val stroke = Stroke(width = strokeWidth.toPx(), join = StrokeJoin.Round, cap = StrokeCap.Round)
        val w = size.width
        val h = size.height
        val knobRadius = w * 0.14f
        val path = Path().apply {
            moveTo(w * 0.18f, h * 0.18f)
            lineTo(w * 0.62f, h * 0.18f)
            // Knob bulging outward on the right edge.
            lineTo(w * 0.62f, h * 0.34f)
            arcTo(
                rect = androidx.compose.ui.geometry.Rect(
                    w * 0.62f,
                    h * 0.5f - knobRadius,
                    w * 0.62f + knobRadius * 2f,
                    h * 0.5f + knobRadius,
                ),
                startAngleDegrees = 180f,
                sweepAngleDegrees = -180f,
                forceMoveTo = false,
            )
            lineTo(w * 0.62f, h * 0.66f)
            lineTo(w * 0.82f, h * 0.66f)
            lineTo(w * 0.82f, h * 0.82f)
            lineTo(w * 0.5f + knobRadius, h * 0.82f)
            // Socket indenting inward on the bottom edge.
            arcTo(
                rect = androidx.compose.ui.geometry.Rect(
                    w * 0.5f - knobRadius,
                    h * 0.82f - knobRadius,
                    w * 0.5f + knobRadius,
                    h * 0.82f + knobRadius,
                ),
                startAngleDegrees = 0f,
                sweepAngleDegrees = 180f,
                forceMoveTo = false,
            )
            lineTo(w * 0.18f, h * 0.82f)
            close()
        }
        drawPath(path, color = tint, style = stroke)
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

/** An outline five-point star — the document preview pane's "favorite" action. */
@Composable
fun StarGlyph(modifier: Modifier = Modifier, tint: Color = AppColors.textMuted, strokeWidth: Dp = 1.5.dp) {
    Canvas(modifier = modifier) {
        val cx = size.width / 2f
        val cy = size.height / 2f
        val outerRadius = size.minDimension * 0.46f
        val innerRadius = outerRadius * 0.42f
        val path = Path()
        for (i in 0 until 10) {
            val angle = (PI / 5 * i - PI / 2).toFloat()
            val r = if (i % 2 == 0) outerRadius else innerRadius
            val point = Offset(cx + r * cos(angle), cy + r * sin(angle))
            if (i == 0) path.moveTo(point.x, point.y) else path.lineTo(point.x, point.y)
        }
        path.close()
        drawPath(path, color = tint, style = Stroke(width = strokeWidth.toPx(), join = StrokeJoin.Round))
    }
}

/** A download arrow over a tray line — the document preview pane's "download" action. */
@Composable
fun DownloadGlyph(modifier: Modifier = Modifier, tint: Color = AppColors.textMuted, strokeWidth: Dp = 1.6.dp) {
    Canvas(modifier = modifier) {
        val stroke = Stroke(width = strokeWidth.toPx(), cap = StrokeCap.Round, join = StrokeJoin.Round)
        val cx = size.width / 2f
        val arrow = Path().apply {
            moveTo(cx, size.height * 0.08f)
            lineTo(cx, size.height * 0.62f)
            moveTo(size.width * 0.28f, size.height * 0.42f)
            lineTo(cx, size.height * 0.68f)
            lineTo(size.width * 0.72f, size.height * 0.42f)
        }
        drawPath(arrow, color = tint, style = stroke)
        drawLine(
            color = tint,
            start = Offset(size.width * 0.14f, size.height * 0.88f),
            end = Offset(size.width * 0.86f, size.height * 0.88f),
            strokeWidth = stroke.width,
            cap = StrokeCap.Round,
        )
    }
}

/** Three horizontal dots — a row/card's "more actions" overflow menu trigger. */
@Composable
fun MoreGlyph(modifier: Modifier = Modifier, tint: Color = AppColors.textMuted, dotRadius: Dp = 1.6.dp) {
    Canvas(modifier = modifier) {
        val r = dotRadius.toPx()
        val y = size.height / 2f
        listOf(size.width * 0.2f, size.width * 0.5f, size.width * 0.8f).forEach { x ->
            drawCircle(color = tint, radius = r, center = Offset(x, y))
        }
    }
}

/** A filled, right-pointing triangle: "start" / "run". */
@Composable
fun PlayGlyph(modifier: Modifier = Modifier, tint: Color = AppColors.textMuted) {
    Canvas(modifier = modifier) {
        val path = Path().apply {
            moveTo(size.width * 0.18f, size.height * 0.08f)
            lineTo(size.width * 0.92f, size.height * 0.5f)
            lineTo(size.width * 0.18f, size.height * 0.92f)
            close()
        }
        drawPath(path = path, color = tint)
    }
}

/**
 * A plus sign — the upload-document action's leading icon. Previously a raw
 * `"+"` character rendered in the platform's default font (the one place a
 * system font leaked through); drawn here instead so every icon in the app,
 * hand-drawn or not, comes from one visual system at one consistent weight.
 */
@Composable
fun PlusGlyph(modifier: Modifier = Modifier, tint: Color = AppColors.textMuted, strokeWidth: Dp = 1.8.dp) {
    Canvas(modifier = modifier) {
        val stroke = Stroke(width = strokeWidth.toPx(), cap = StrokeCap.Round)
        val cx = size.width / 2f
        val cy = size.height / 2f
        drawLine(color = tint, start = Offset(cx, size.height * 0.08f), end = Offset(cx, size.height * 0.92f), strokeWidth = stroke.width, cap = StrokeCap.Round)
        drawLine(color = tint, start = Offset(size.width * 0.08f, cy), end = Offset(size.width * 0.92f, cy), strokeWidth = stroke.width, cap = StrokeCap.Round)
    }
}

/**
 * An "x" — a close/clear affordance, e.g. [com.peltoche.fyde.components.form.AppSearchField]'s
 * clear button. Previously a raw `"×"` character; see [PlusGlyph]'s doc
 * comment for why that's a problem this glyph fixes.
 */
@Composable
fun CloseGlyph(modifier: Modifier = Modifier, tint: Color = AppColors.textMuted, strokeWidth: Dp = 1.6.dp) {
    Canvas(modifier = modifier) {
        val stroke = Stroke(width = strokeWidth.toPx(), cap = StrokeCap.Round)
        val inset = size.minDimension * 0.18f
        drawLine(color = tint, start = Offset(inset, inset), end = Offset(size.width - inset, size.height - inset), strokeWidth = stroke.width, cap = StrokeCap.Round)
        drawLine(color = tint, start = Offset(size.width - inset, inset), end = Offset(inset, size.height - inset), strokeWidth = stroke.width, cap = StrokeCap.Round)
    }
}

/**
 * An exclamation mark — the connect-failed error state's icon. Previously a
 * raw `"!"` character; see [PlusGlyph]'s doc comment for why that's a
 * problem this glyph fixes. Already sits inside [com.peltoche.fyde.components.feedback.AppErrorState]'s
 * own tinted circle badge, so this draws only the mark itself, not a
 * second circle around it.
 */
@Composable
fun AlertGlyph(modifier: Modifier = Modifier, tint: Color = AppColors.error, strokeWidth: Dp = 2.2.dp) {
    Canvas(modifier = modifier) {
        val stroke = Stroke(width = strokeWidth.toPx(), cap = StrokeCap.Round)
        val cx = size.width / 2f
        drawLine(
            color = tint,
            start = Offset(cx, size.height * 0.04f),
            end = Offset(cx, size.height * 0.62f),
            strokeWidth = stroke.width,
            cap = StrokeCap.Round,
        )
        drawCircle(color = tint, radius = stroke.width * 0.6f, center = Offset(cx, size.height * 0.88f))
    }
}
