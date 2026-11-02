package com.peltoche.fyde.components.feedback

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.size
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.peltoche.fyde.AlertGlyph
import com.peltoche.fyde.CheckGlyph
import com.peltoche.fyde.ClockGlyph
import com.peltoche.fyde.theme.AppColors
import com.peltoche.fyde.theme.AppIconSize
import com.peltoche.fyde.theme.AppSpacing
import com.peltoche.fyde.theme.AppTypography

/** The five states a source's status line can be in (design-system.md § Source status). */
enum class SourceStatusKind { Running, Waiting, Failed, UpToDate, Queued }

/**
 * A source's one-line status (S1's rows, S5's header, D4's État column):
 * a 12dp glyph and [text] in Medium, both in the state's colour — a spinner
 * in `primary` while running, a clock in `warning` while it waits for a code,
 * an alert in `error` with the reason it failed, a check in `success` once up
 * to date, a muted clock while queued behind another run.
 */
@Composable
fun SourceStatus(kind: SourceStatusKind, text: String, modifier: Modifier = Modifier) {
    val color = sourceStatusColor(kind)
    Row(
        modifier = modifier,
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(AppSpacing.space2),
    ) {
        val iconModifier = Modifier.size(AppIconSize.xs)
        when (kind) {
            SourceStatusKind.Running -> CircularProgressIndicator(modifier = iconModifier, strokeWidth = 1.5.dp, color = color)
            SourceStatusKind.Waiting, SourceStatusKind.Queued -> ClockGlyph(modifier = iconModifier, tint = color)
            SourceStatusKind.Failed -> AlertGlyph(modifier = iconModifier, tint = color, strokeWidth = 1.6.dp)
            SourceStatusKind.UpToDate -> CheckGlyph(modifier = iconModifier, tint = color)
        }
        Text(
            text = text,
            style = AppTypography.caption.copy(fontWeight = FontWeight.Medium),
            color = color,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
        )
    }
}

@Composable
private fun sourceStatusColor(kind: SourceStatusKind): Color = when (kind) {
    SourceStatusKind.Running -> AppColors.primary
    SourceStatusKind.Waiting -> AppColors.warning
    SourceStatusKind.Failed -> AppColors.error
    SourceStatusKind.UpToDate -> AppColors.success
    SourceStatusKind.Queued -> AppColors.textMuted
}
