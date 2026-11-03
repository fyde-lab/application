package com.peltoche.fyde.components.content

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import com.peltoche.fyde.theme.AppColors
import com.peltoche.fyde.theme.AppDimensions
import com.peltoche.fyde.theme.AppRadius
import com.peltoche.fyde.theme.AppSpacing
import com.peltoche.fyde.theme.AppTypography

/** One row of [DocumentAttributes]: "Émise le" and "10 octobre 2026"; an [isAmount] value is set in `data`. */
data class DocumentAttributeRow(val label: String, val value: String, val isAmount: Boolean = false)

/**
 * A document's Informations section (V6, D2 — design-system.md
 * § Informations): [heading] in small capitals, then a `surface` group of
 * label / value rows. A long value wraps under its own half of the row,
 * never truncated.
 */
@Composable
fun DocumentAttributes(heading: String, rows: List<DocumentAttributeRow>, modifier: Modifier = Modifier) {
    Column(modifier = modifier.fillMaxWidth()) {
        Text(
            text = heading.uppercase(),
            style = AppTypography.caption.copy(fontWeight = FontWeight.SemiBold),
            color = AppColors.textMuted,
            modifier = Modifier
                .padding(horizontal = AppSpacing.space2)
                .padding(bottom = AppSpacing.space3)
                .semantics { heading() },
        )
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .clip(AppRadius.lg)
                .background(AppColors.surface)
                .border(AppDimensions.borderHairline, AppColors.border, AppRadius.lg),
        ) {
            rows.forEachIndexed { index, row ->
                if (index > 0) AppDivider()
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .semantics(mergeDescendants = true) {}
                        .heightIn(min = AppDimensions.rowHeightCompact)
                        .padding(horizontal = AppSpacing.space5, vertical = AppSpacing.space3),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(AppSpacing.space4),
                ) {
                    Text(text = row.label, style = AppTypography.body, color = AppColors.textMuted, modifier = Modifier.weight(1f))
                    Text(
                        text = row.value,
                        style = if (row.isAmount) AppTypography.data else AppTypography.body,
                        color = AppColors.text,
                        textAlign = TextAlign.End,
                        modifier = Modifier.weight(1f, fill = false),
                    )
                }
            }
        }
    }
}
