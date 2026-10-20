package com.peltoche.fyde.components.feedback

import androidx.compose.foundation.layout.height
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.unit.dp
import com.peltoche.fyde.theme.AppColors
import com.peltoche.fyde.theme.AppDimensions
import com.peltoche.fyde.theme.AppRadius

/**
 * A horizontal bar for progress and for meters (how much of a limit is
 * used): determinate for a known [progress] in `0..1`, indeterminate when
 * it's `null`. design-system.md § Shape: a rectangle with `radius-xs`
 * corners and square ends on a `border` track — not M3's own indicator,
 * whose rounded ends, track gap and stop dot read as a different control.
 * Found via review: the usage meters were hand-drawn to that rule while a
 * running script's progress used the stock indicator, so the app had two
 * different bars.
 *
 * [color] carries the state (`primary` normally; `warning`/`error` for a
 * meter near or over its limit). Never rely on it alone — say the state in
 * text next to the bar too.
 */
@Composable
fun AppProgressBar(
    progress: Float?,
    modifier: Modifier = Modifier,
    color: Color = AppColors.primary,
) {
    val barModifier = modifier.height(AppDimensions.progressBarHeight).clip(AppRadius.xs)
    if (progress != null) {
        LinearProgressIndicator(
            progress = { progress.coerceIn(0f, 1f) },
            modifier = barModifier,
            color = color,
            trackColor = AppColors.border,
            strokeCap = StrokeCap.Butt,
            gapSize = 0.dp,
            drawStopIndicator = {},
        )
    } else {
        LinearProgressIndicator(
            modifier = barModifier,
            color = color,
            trackColor = AppColors.border,
            strokeCap = StrokeCap.Butt,
            gapSize = 0.dp,
        )
    }
}
