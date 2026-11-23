package com.peltoche.fyde.icons

import androidx.compose.foundation.Canvas
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.geometry.RoundRect
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.translate
import androidx.compose.ui.graphics.drawscope.scale
import androidx.compose.ui.graphics.vector.PathParser
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.peltoche.fyde.theme.AppColors

/**
 * The folder icons and the vault's own controls, traced from `i-xxx` in
 * `design/proposals/000-original-design/assets/mockups/icons.svg` (gift, id card, scale, paw, ticket,
 * graduation cap and car have no symbol there and are drawn in the same hand). Unlike `NavGlyphs.kt`'s hand-placed shapes, these replay the
 * SVG's own 24-unit paths, scaled to the glyph's size.
 */

@Composable
fun HomeGlyph(modifier: Modifier = Modifier, tint: Color = AppColors.textMuted, strokeWidth: Dp = 1.6.dp) =
    PathGlyph(HomePaths, modifier, tint, strokeWidth)

@Composable
fun WalletGlyph(modifier: Modifier = Modifier, tint: Color = AppColors.textMuted, strokeWidth: Dp = 1.6.dp) =
    PathGlyph(WalletPaths, modifier, tint, strokeWidth)

@Composable
fun BriefcaseGlyph(modifier: Modifier = Modifier, tint: Color = AppColors.textMuted, strokeWidth: Dp = 1.6.dp) =
    PathGlyph(BriefcasePaths, modifier, tint, strokeWidth)

@Composable
fun LandmarkGlyph(modifier: Modifier = Modifier, tint: Color = AppColors.textMuted, strokeWidth: Dp = 1.6.dp) =
    PathGlyph(LandmarkPaths, modifier, tint, strokeWidth)

@Composable
fun HeartGlyph(modifier: Modifier = Modifier, tint: Color = AppColors.textMuted, strokeWidth: Dp = 1.6.dp) =
    PathGlyph(HeartPaths, modifier, tint, strokeWidth)

@Composable
fun TrainGlyph(modifier: Modifier = Modifier, tint: Color = AppColors.textMuted, strokeWidth: Dp = 1.6.dp) =
    PathGlyph(TrainPaths, modifier, tint, strokeWidth)

@Composable
fun BagGlyph(modifier: Modifier = Modifier, tint: Color = AppColors.textMuted, strokeWidth: Dp = 1.6.dp) =
    PathGlyph(BagPaths, modifier, tint, strokeWidth)

@Composable
fun UsersGlyph(modifier: Modifier = Modifier, tint: Color = AppColors.textMuted, strokeWidth: Dp = 1.6.dp) =
    PathGlyph(UsersPaths, modifier, tint, strokeWidth)

@Composable
fun RepeatGlyph(modifier: Modifier = Modifier, tint: Color = AppColors.textMuted, strokeWidth: Dp = 1.6.dp) =
    PathGlyph(RepeatPaths, modifier, tint, strokeWidth)

@Composable
fun GiftGlyph(modifier: Modifier = Modifier, tint: Color = AppColors.textMuted, strokeWidth: Dp = 1.6.dp) =
    PathGlyph(GiftPaths, modifier, tint, strokeWidth)

@Composable
fun IdCardGlyph(modifier: Modifier = Modifier, tint: Color = AppColors.textMuted, strokeWidth: Dp = 1.6.dp) =
    PathGlyph(IdCardPaths, modifier, tint, strokeWidth)

@Composable
fun ScaleGlyph(modifier: Modifier = Modifier, tint: Color = AppColors.textMuted, strokeWidth: Dp = 1.6.dp) =
    PathGlyph(ScalePaths, modifier, tint, strokeWidth)

@Composable
fun PawGlyph(modifier: Modifier = Modifier, tint: Color = AppColors.textMuted, strokeWidth: Dp = 1.6.dp) =
    PathGlyph(PawPaths, modifier, tint, strokeWidth)

@Composable
fun TicketGlyph(modifier: Modifier = Modifier, tint: Color = AppColors.textMuted, strokeWidth: Dp = 1.6.dp) =
    PathGlyph(TicketPaths, modifier, tint, strokeWidth)

@Composable
fun GraduationCapGlyph(modifier: Modifier = Modifier, tint: Color = AppColors.textMuted, strokeWidth: Dp = 1.6.dp) =
    PathGlyph(GraduationCapPaths, modifier, tint, strokeWidth)

@Composable
fun CarGlyph(modifier: Modifier = Modifier, tint: Color = AppColors.textMuted, strokeWidth: Dp = 1.6.dp) =
    PathGlyph(CarPaths, modifier, tint, strokeWidth)

@Composable
fun CalendarGlyph(modifier: Modifier = Modifier, tint: Color = AppColors.textMuted, strokeWidth: Dp = 1.6.dp) =
    PathGlyph(CalendarPaths, modifier, tint, strokeWidth)

/** A folder: a subfolder's mark, and a top-level folder's plainest icon. */
@Composable
fun FolderGlyph(modifier: Modifier = Modifier, tint: Color = AppColors.textMuted, strokeWidth: Dp = 1.6.dp) =
    PathGlyph(FolderPaths, modifier, tint, strokeWidth)

/** The Coffre destination: its folder grid. */
@Composable
fun GridGlyph(modifier: Modifier = Modifier, tint: Color = AppColors.textMuted, strokeWidth: Dp = 1.6.dp) =
    PathGlyph(GridPaths, modifier, tint, strokeWidth)

/** A category page's newest/oldest toggle. */
@Composable
fun SortGlyph(modifier: Modifier = Modifier, tint: Color = AppColors.textMuted, strokeWidth: Dp = 1.6.dp) =
    PathGlyph(SortPaths, modifier, tint, strokeWidth)

/** The Sources destination. */
@Composable
fun PlugGlyph(modifier: Modifier = Modifier, tint: Color = AppColors.textMuted, strokeWidth: Dp = 1.6.dp) =
    PathGlyph(PlugPaths, modifier, tint, strokeWidth)

/** Rangement automatique: Fyde files by itself (design-system.md § A source's folder). */
@Composable
fun SparkleGlyph(modifier: Modifier = Modifier, tint: Color = AppColors.textMuted, strokeWidth: Dp = 1.6.dp) =
    PathGlyph(SparklePaths, modifier, tint, strokeWidth)

@Composable
private fun PathGlyph(paths: List<Path>, modifier: Modifier, tint: Color, strokeWidth: Dp) {
    Canvas(modifier = modifier) {
        val unit = size.minDimension / VIEWBOX
        val stroke = Stroke(width = strokeWidth.toPx() / unit, join = StrokeJoin.Round, cap = StrokeCap.Round)
        translate((size.width - VIEWBOX * unit) / 2f, (size.height - VIEWBOX * unit) / 2f) {
            scale(unit, unit, pivot = Offset.Zero) {
                paths.forEach { drawPath(it, color = tint, style = stroke) }
            }
        }
    }
}

private const val VIEWBOX = 24f

private fun svg(d: String): Path = PathParser().parsePathString(d).toPath()

private fun rect(x: Float, y: Float, width: Float, height: Float, radius: Float): Path = Path().apply {
    addRoundRect(RoundRect(Rect(x, y, x + width, y + height), CornerRadius(radius)))
}

private fun circle(cx: Float, cy: Float, r: Float): Path = Path().apply {
    addOval(Rect(cx - r, cy - r, cx + r, cy + r))
}

private val HomePaths = listOf(svg("M4 10.5 12 4l8 6.5V20h-5.5v-6h-5v6H4z"))

private val WalletPaths = listOf(
    svg("M4 7.5h14a2 2 0 0 1 2 2v9a2 2 0 0 1-2 2H5a1 1 0 0 1-1-1V6a2 2 0 0 1 2-2h11v3.5"),
    svg("M16 14h1.5"),
)

private val BriefcasePaths = listOf(
    rect(3.5f, 7.5f, 17f, 12f, 2f),
    svg("M9 7.5V5.5a1 1 0 0 1 1-1h4a1 1 0 0 1 1 1v2M3.5 12.5h17"),
)

private val LandmarkPaths = listOf(svg("M3 9.5 12 4l9 5.5M5 10v7M9.5 10v7M14.5 10v7M19 10v7M3 20h18"))

private val HeartPaths = listOf(
    svg("M12 20s-7.5-4.6-7.5-10.2A4.3 4.3 0 0 1 12 7.4a4.3 4.3 0 0 1 7.5 2.4C19.5 15.4 12 20 12 20z"),
)

private val TrainPaths = listOf(
    rect(5f, 3.5f, 14f, 14f, 3f),
    svg("M5 11h14M8.5 21l2-3.5M15.5 21l-2-3.5M9 14.5h.1M15 14.5h.1"),
)

private val BagPaths = listOf(svg("M5 8h14l-1 12.5H6z"), svg("M9 8V6.5a3 3 0 0 1 6 0V8"))

private val UsersPaths = listOf(
    circle(9f, 8.5f, 3.5f),
    svg("M3 20c0-3.3 2.7-5.5 6-5.5s6 2.2 6 5.5M16 5.2a3.5 3.5 0 0 1 0 6.6M18 14.8c1.8.8 3 2.6 3 5.2"),
)

private val RepeatPaths = listOf(svg("M17 3.5 20 6.5 17 9.5M4 11.5v-1a4 4 0 0 1 4-4h12M7 20.5 4 17.5l3-3M20 12.5v1a4 4 0 0 1-4 4H4"))

private val GiftPaths = listOf(
    rect(4f, 9f, 16f, 3.5f, 1f),
    svg("M5.5 12.5V19a1 1 0 0 0 1 1h11a1 1 0 0 0 1-1v-6.5M12 9v11"),
    svg("M12 9C10.5 5.5 7 5.5 7.5 7.5 7.9 9 12 9 12 9zM12 9c1.5-3.5 5-3.5 4.5-1.5C16.1 9 12 9 12 9z"),
)

private val IdCardPaths = listOf(
    rect(3f, 5.5f, 18f, 13f, 2f),
    circle(8.5f, 10.5f, 2f),
    svg("M5.5 15.5c.5-1.5 1.6-2.3 3-2.3s2.5.8 3 2.3M14 10h4.5M14 13.5h3"),
)

private val ScalePaths = listOf(
    svg("M12 4v16M8 20h8M4 7.5h16"),
    svg("M4 7.5 1.5 14a2.5 2.5 0 0 0 5 0zM20 7.5 17.5 14a2.5 2.5 0 0 0 5 0z"),
)

private val PawPaths = listOf(
    circle(6.5f, 10.5f, 1.7f),
    circle(9.7f, 6.3f, 1.8f),
    circle(14.3f, 6.3f, 1.8f),
    circle(17.5f, 10.5f, 1.7f),
    svg("M12 12c-2.5 0-5 3.5-5 5.5 0 1.5 1.2 2.5 2.7 2.5.9 0 1.5-.5 2.3-.5s1.4.5 2.3.5c1.5 0 2.7-1 2.7-2.5 0-2-2.5-5.5-5-5.5z"),
)

private val TicketPaths = listOf(
    svg("M3.5 8a1 1 0 0 1 1-1h15a1 1 0 0 1 1 1v2.5a1.5 1.5 0 0 0 0 3V16a1 1 0 0 1-1 1h-15a1 1 0 0 1-1-1v-2.5a1.5 1.5 0 0 0 0-3z"),
    svg("M14.5 7v2M14.5 11v2M14.5 15v2"),
)

private val GraduationCapPaths = listOf(
    svg("M2.5 9.5 12 5l9.5 4.5L12 14z"),
    svg("M6.5 11.6V16c0 1.5 2.5 3 5.5 3s5.5-1.5 5.5-3v-4.4M21.5 9.5V15"),
)

private val CarPaths = listOf(
    svg("M3.5 17v-4.5l2.2-5.5h12.6l2.2 5.5V17M3.5 12.5h17"),
    svg("M5.7 17.5a1.8 1.8 0 1 0 3.6 0a1.8 1.8 0 1 0-3.6 0M14.7 17.5a1.8 1.8 0 1 0 3.6 0a1.8 1.8 0 1 0-3.6 0"),
)

private val CalendarPaths = listOf(rect(4f, 5f, 16f, 15f, 2f), svg("M4 10h16M8.5 3v4M15.5 3v4"))

private val FolderPaths = listOf(
    svg("M3.5 7a1.5 1.5 0 0 1 1.5-1.5h4.5l2 2.5H19a1.5 1.5 0 0 1 1.5 1.5V18a1.5 1.5 0 0 1-1.5 1.5H5A1.5 1.5 0 0 1 3.5 18z"),
)

private val GridPaths = listOf(
    rect(4f, 4f, 6.5f, 6.5f, 1f),
    rect(13.5f, 4f, 6.5f, 6.5f, 1f),
    rect(4f, 13.5f, 6.5f, 6.5f, 1f),
    rect(13.5f, 13.5f, 6.5f, 6.5f, 1f),
)

private val PlugPaths = listOf(svg("M9 3v4M15 3v4M6.5 7h11v4a5.5 5.5 0 0 1-11 0zM12 16.5V21"))

private val SortPaths = listOf(svg("M7 4.5v15M3.5 16 7 19.5l3.5-3.5M17 19.5v-15M13.5 8 17 4.5 20.5 8"))

private val SparklePaths = listOf(
    svg("M12 3.5v4M12 16.5v4M3.5 12h4M16.5 12h4M6.2 6.2l2.6 2.6M15.2 15.2l2.6 2.6M17.8 6.2l-2.6 2.6M8.8 15.2l-2.6 2.6"),
)
