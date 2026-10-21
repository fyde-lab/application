package com.peltoche.fyde

import androidx.compose.foundation.Canvas
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.peltoche.fyde.theme.AppColors
import kotlin.math.PI
import kotlin.math.atan2
import kotlin.math.cos
import kotlin.math.sin
import kotlin.math.sqrt

/**
 * Hand-drawn `Canvas` glyphs for the navigation and list rows, in the
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

/**
 * A padlock: a narrow, tall shackle over a body with a keyhole. Found via
 * review: the shackle used to be a wide, flat arch over an empty rectangle,
 * which at the 18dp the encryption badge draws it read as an envelope or a
 * basket rather than a lock.
 */
@Composable
fun LockGlyph(modifier: Modifier = Modifier, tint: Color = AppColors.success, strokeWidth: Dp = 1.6.dp) {
    Canvas(modifier = modifier) {
        val stroke = Stroke(width = strokeWidth.toPx(), join = StrokeJoin.Round, cap = StrokeCap.Round)
        val bodyTop = size.height * 0.46f
        drawRoundRect(
            color = tint,
            topLeft = Offset(size.width * 0.18f, bodyTop),
            size = Size(size.width * 0.64f, size.height * 0.46f),
            cornerRadius = CornerRadius(size.minDimension * 0.08f),
            style = stroke,
        )
        val shacklePath = Path().apply {
            val shackleWidth = size.width * 0.40f
            val left = (size.width - shackleWidth) / 2f
            val archBottom = size.height * 0.30f
            moveTo(left, bodyTop)
            lineTo(left, archBottom)
            arcTo(
                rect = Rect(left, archBottom - shackleWidth / 2f, left + shackleWidth, archBottom + shackleWidth / 2f),
                startAngleDegrees = 180f,
                sweepAngleDegrees = 180f,
                forceMoveTo = false,
            )
            lineTo(left + shackleWidth, bodyTop)
        }
        drawPath(shacklePath, color = tint, style = stroke)
        drawCircle(color = tint, radius = size.minDimension * 0.07f, center = Offset(size.width / 2f, size.height * 0.67f))
    }
}

/**
 * A cog: one closed outline of eight flat-topped teeth around a hole. Found
 * via review: it used to be a circle with eight separate rays, which reads
 * as a sun (a brightness/theme control), not as Settings.
 */
@Composable
fun GearGlyph(modifier: Modifier = Modifier, tint: Color = AppColors.textMuted, strokeWidth: Dp = 1.4.dp) {
    Canvas(modifier = modifier) {
        val stroke = Stroke(width = strokeWidth.toPx(), join = StrokeJoin.Round, cap = StrokeCap.Round)
        val center = Offset(size.width / 2f, size.height / 2f)
        val outerRadius = size.minDimension * 0.46f
        val rootRadius = size.minDimension * 0.35f
        val teeth = 8
        val pitch = 2 * PI / teeth
        // Half-widths, in radians, of a tooth at its root and at its tip:
        // narrower at the tip, so each tooth tapers slightly.
        val rootHalf = pitch * 0.30
        val tipHalf = pitch * 0.20
        fun point(radius: Float, angle: Double) =
            Offset(center.x + radius * cos(angle).toFloat(), center.y + radius * sin(angle).toFloat())
        val outline = Path()
        repeat(teeth) { i ->
            val angle = i * pitch - PI / 2
            val corners = listOf(
                point(rootRadius, angle - rootHalf),
                point(outerRadius, angle - tipHalf),
                point(outerRadius, angle + tipHalf),
                point(rootRadius, angle + rootHalf),
            )
            corners.forEachIndexed { index, corner ->
                if (i == 0 && index == 0) outline.moveTo(corner.x, corner.y) else outline.lineTo(corner.x, corner.y)
            }
        }
        outline.close()
        drawPath(outline, color = tint, style = stroke)
        drawCircle(color = tint, radius = size.minDimension * 0.14f, center = center, style = stroke)
    }
}

/** Two angle brackets, `<` `>` — a code glyph; no destination uses it since Scripts took [PuzzleGlyph]. */
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
 * A puzzle piece — the script grid's
 * per-card icon chip: a square body with a round knob on its top and right
 * edges, as one closed outline like every other glyph here. Found via
 * review: it used to close its right knob with a diagonal back into the
 * body and widen the body halfway down, so it read as a lopsided L-shape
 * rather than a puzzle piece.
 */
@Composable
fun PuzzleGlyph(modifier: Modifier = Modifier, tint: Color = AppColors.textMuted, strokeWidth: Dp = 1.4.dp) {
    Canvas(modifier = modifier) {
        val stroke = Stroke(width = strokeWidth.toPx(), join = StrokeJoin.Round, cap = StrokeCap.Round)
        val knobRadius = size.minDimension * 0.16f
        // How far each knob's center sits outside the edge it grows from:
        // less than its radius, so the knob joins the edge through a neck
        // narrower than the knob itself.
        val knobOffset = size.minDimension * 0.10f
        // Body plus knobs span the whole canvas, edge to edge like
        // [DocumentGlyph]. Found via review: inset
        // to 10%-90% it read visibly smaller than its neighbours.
        val reach = knobRadius + knobOffset
        val left = 0f
        val right = size.width - reach
        val top = reach
        val bottom = size.height
        val neckHalf = sqrt(knobRadius * knobRadius - knobOffset * knobOffset)
        // Angle between the edge and the radius to where the knob meets it.
        val neckAngle = (atan2(knobOffset, neckHalf) * 180f / PI.toFloat())
        val topKnob = Offset((left + right) / 2f, top - knobOffset)
        val rightKnob = Offset(right + knobOffset, (top + bottom) / 2f)
        fun Path.knob(center: Offset, outwardDegrees: Float) = arcTo(
            rect = Rect(center, knobRadius),
            startAngleDegrees = outwardDegrees - 90f - neckAngle,
            sweepAngleDegrees = 180f + 2f * neckAngle,
            forceMoveTo = false,
        )
        val path = Path().apply {
            moveTo(left, top)
            lineTo(topKnob.x - neckHalf, top)
            knob(topKnob, outwardDegrees = 270f)
            lineTo(right, top)
            lineTo(right, rightKnob.y - neckHalf)
            knob(rightKnob, outwardDegrees = 0f)
            lineTo(right, bottom)
            lineTo(left, bottom)
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

/**
 * A lightning bolt — Welcome's "fetched by itself" promise. Traced from
 * `i-bolt` in `design/assets/mockups/icons.svg`, on its 24-unit grid.
 */
@Composable
fun BoltGlyph(modifier: Modifier = Modifier, tint: Color = AppColors.textMuted, strokeWidth: Dp = 1.4.dp) {
    Canvas(modifier = modifier) {
        val unit = size.minDimension / 24f
        val path = Path().apply {
            moveTo(13f * unit, 2.5f * unit)
            lineTo(4.5f * unit, 13.5f * unit)
            lineTo(11f * unit, 13.5f * unit)
            lineTo(10f * unit, 21.5f * unit)
            lineTo(18.5f * unit, 10.5f * unit)
            lineTo(12f * unit, 10.5f * unit)
            close()
        }
        drawPath(path, color = tint, style = Stroke(width = strokeWidth.toPx(), join = StrokeJoin.Round, cap = StrokeCap.Round))
    }
}

/**
 * Two arrows chasing each other round a circle — sync across devices
 * (Welcome's "on every device" promise). Traced from `i-sync` in
 * `design/assets/mockups/icons.svg`, on its 24-unit grid.
 */
@Composable
fun SyncGlyph(modifier: Modifier = Modifier, tint: Color = AppColors.textMuted, strokeWidth: Dp = 1.4.dp) {
    Canvas(modifier = modifier) {
        val unit = size.minDimension / 24f
        val circle = Rect(Offset(12f * unit, 12f * unit), 8f * unit)
        val path = Path().apply {
            // Upper arc, right to left, ending in the top-left arrowhead.
            arcTo(circle, startAngleDegrees = -7f, sweepAngleDegrees = -132f, forceMoveTo = true)
            lineTo(4f * unit, 8.5f * unit)
            moveTo(4f * unit, 4f * unit)
            lineTo(4f * unit, 8.5f * unit)
            lineTo(8.5f * unit, 8.5f * unit)
            // Lower arc, left to right, ending in the bottom-right arrowhead.
            arcTo(circle, startAngleDegrees = 173f, sweepAngleDegrees = -132f, forceMoveTo = true)
            lineTo(20f * unit, 15.5f * unit)
            moveTo(20f * unit, 20f * unit)
            lineTo(20f * unit, 15.5f * unit)
            lineTo(15.5f * unit, 15.5f * unit)
        }
        drawPath(path, color = tint, style = Stroke(width = strokeWidth.toPx(), join = StrokeJoin.Round, cap = StrokeCap.Round))
    }
}

/** A left chevron — [com.peltoche.fyde.components.action.AppBackButton]'s mark. Traced from `i-chev-l` in `icons.svg`. */
@Composable
fun ChevronLeftGlyph(modifier: Modifier = Modifier, tint: Color = AppColors.text, strokeWidth: Dp = 1.6.dp) {
    Canvas(modifier = modifier) {
        val unit = size.minDimension / 24f
        val path = Path().apply {
            moveTo(15f * unit, 6f * unit)
            lineTo(9f * unit, 12f * unit)
            lineTo(15f * unit, 18f * unit)
        }
        drawPath(path, color = tint, style = Stroke(width = strokeWidth.toPx(), join = StrokeJoin.Round, cap = StrokeCap.Round))
    }
}

/** A clock face — waiting, expiry (a warning note's mark). Traced from `i-clock` in `icons.svg`. */
@Composable
fun ClockGlyph(modifier: Modifier = Modifier, tint: Color = AppColors.warning, strokeWidth: Dp = 1.6.dp) {
    Canvas(modifier = modifier) {
        val unit = size.minDimension / 24f
        val stroke = Stroke(width = strokeWidth.toPx(), join = StrokeJoin.Round, cap = StrokeCap.Round)
        drawCircle(color = tint, radius = 8.5f * unit, center = Offset(12f * unit, 12f * unit), style = stroke)
        val hands = Path().apply {
            moveTo(12f * unit, 7.5f * unit)
            lineTo(12f * unit, 12f * unit)
            lineTo(15f * unit, 14f * unit)
        }
        drawPath(hands, color = tint, style = stroke)
    }
}

/**
 * An eye — "show password"; with [crossed], "hide password". Traced from
 * `i-eye` in `icons.svg`, the crossed variant as that eye under `i-eye-off`'s
 * diagonal.
 */
@Composable
fun EyeGlyph(modifier: Modifier = Modifier, tint: Color = AppColors.textMuted, crossed: Boolean = false, strokeWidth: Dp = 1.6.dp) {
    Canvas(modifier = modifier) {
        val unit = size.minDimension / 24f
        val stroke = Stroke(width = strokeWidth.toPx(), join = StrokeJoin.Round, cap = StrokeCap.Round)
        val outline = Path().apply {
            moveTo(2.5f * unit, 12f * unit)
            quadraticTo(12f * unit, -0.5f * unit, 21.5f * unit, 12f * unit)
            quadraticTo(12f * unit, 24.5f * unit, 2.5f * unit, 12f * unit)
            close()
        }
        drawPath(outline, color = tint, style = stroke)
        drawCircle(color = tint, radius = 3f * unit, center = Offset(12f * unit, 12f * unit), style = stroke)
        if (crossed) {
            drawLine(color = tint, start = Offset(3f * unit, 3f * unit), end = Offset(21f * unit, 21f * unit), strokeWidth = stroke.width, cap = StrokeCap.Round)
        }
    }
}

/**
 * A warning triangle with an exclamation mark — a risk the user has to
 * accept (O4's insecure-storage sheet). Traced from `i-alert` in `icons.svg`.
 */
@Composable
fun AlertTriangleGlyph(modifier: Modifier = Modifier, tint: Color = AppColors.warning, strokeWidth: Dp = 1.7.dp) {
    Canvas(modifier = modifier) {
        val unit = size.minDimension / 24f
        val stroke = Stroke(width = strokeWidth.toPx(), join = StrokeJoin.Round, cap = StrokeCap.Round)
        val triangle = Path().apply {
            moveTo(12f * unit, 3.5f * unit)
            lineTo(2.5f * unit, 20f * unit)
            lineTo(21.5f * unit, 20f * unit)
            close()
        }
        drawPath(triangle, color = tint, style = stroke)
        val mark = Path().apply {
            moveTo(12f * unit, 10f * unit)
            lineTo(12f * unit, 14.5f * unit)
            moveTo(12f * unit, 17.3f * unit)
            lineTo(12f * unit, 17.5f * unit)
        }
        drawPath(mark, color = tint, style = stroke)
    }
}
