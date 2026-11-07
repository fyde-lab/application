package com.peltoche.fyde.components.feedback

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import com.peltoche.fyde.theme.AppColors
import com.peltoche.fyde.theme.AppDimensions
import com.peltoche.fyde.theme.AppRadius
import com.peltoche.fyde.theme.AppSpacing

/**
 * A level shown as [segments] equal bars (O3's password strength): the
 * first [lit] in [color], the rest in `border`. Never rely on the colour
 * alone — say the level in text beside it.
 */
@Composable
fun AppSegmentedMeter(segments: Int, lit: Int, color: Color, modifier: Modifier = Modifier) {
    Row(horizontalArrangement = Arrangement.spacedBy(AppSpacing.space2), modifier = modifier.fillMaxWidth()) {
        repeat(segments) { index ->
            Box(
                modifier = Modifier
                    .weight(1f)
                    .height(AppDimensions.segmentedMeterHeight)
                    .clip(AppRadius.xs)
                    .background(if (index < lit) color else AppColors.border),
            )
        }
    }
}
