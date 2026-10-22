package com.peltoche.fyde.components.feedback

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.semantics.LiveRegionMode
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.liveRegion
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.peltoche.fyde.theme.AppColors
import com.peltoche.fyde.theme.AppDimensions
import com.peltoche.fyde.theme.AppSpacing
import com.peltoche.fyde.theme.AppTypography

/**
 * `PlanQuota` from the design system: how many of the plan's [max] sources
 * a selection uses ([used]), wherever sources are picked (O7, DO7, S2). One
 * row on `surface` with a `border` hairline: [label] (the plan in words),
 * one 16×6 segment per allowed source (`primary` when used), and "n / max"
 * in Plex Mono. Past [MAX_SEGMENTS] the segments are left out and the
 * count says it alone.
 *
 * At the limit ([used] ≥ [max]) the row turns `warning` wash with its
 * segments and count in `warning`; the caller swaps [label] for its
 * "limit reached" wording. Screen readers hear only [description] ("3
 * sources chosen of 5"), announced again as it changes.
 */
@Composable
fun AppPlanQuota(
    label: AnnotatedString,
    used: Int,
    max: Int,
    description: String,
    modifier: Modifier = Modifier,
) {
    val atLimit = used >= max
    val accent = if (atLimit) AppColors.warning else AppColors.primary
    val shape = RoundedCornerShape(16.dp)
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = modifier
            .fillMaxWidth()
            .clip(shape)
            .then(
                if (atLimit) {
                    Modifier.background(AppColors.tintOf(AppColors.warning))
                } else {
                    Modifier
                        .background(AppColors.surface)
                        .border(AppDimensions.borderHairline, AppColors.border, shape)
                },
            )
            .padding(horizontal = AppSpacing.space5, vertical = AppSpacing.space4)
            .clearAndSetSemantics {
                contentDescription = description
                liveRegion = LiveRegionMode.Polite
            },
    ) {
        Text(
            text = label,
            style = if (atLimit) AppTypography.body.copy(fontWeight = FontWeight.SemiBold) else AppTypography.body,
            color = AppColors.text,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier.weight(1f),
        )
        Spacer(Modifier.width(AppSpacing.space3))
        if (max <= MAX_SEGMENTS) {
            Row(horizontalArrangement = Arrangement.spacedBy(3.dp)) {
                repeat(max) { index ->
                    Box(
                        modifier = Modifier
                            .size(width = 16.dp, height = 6.dp)
                            .clip(CircleShape)
                            .background(if (index < used) accent else AppColors.border),
                    )
                }
            }
            Spacer(Modifier.width(AppSpacing.space5))
        }
        Text(
            text = "$used / $max",
            style = AppTypography.data.copy(fontWeight = FontWeight.Medium),
            color = if (atLimit) AppColors.warning else AppColors.text,
        )
    }
}

/** The most segments [AppPlanQuota] draws: beyond, they'd crowd out the label on a phone. */
private const val MAX_SEGMENTS = 10
