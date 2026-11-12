package com.peltoche.fyde.components.action

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.LiveRegionMode
import androidx.compose.ui.semantics.liveRegion
import androidx.compose.ui.semantics.semantics
import com.peltoche.fyde.components.content.AppDivider
import com.peltoche.fyde.icons.CloseGlyph
import com.peltoche.fyde.theme.AppColors
import com.peltoche.fyde.theme.AppIconSize
import com.peltoche.fyde.theme.AppSpacing
import com.peltoche.fyde.theme.AppTypography

/**
 * The foot of a list while documents are ticked (V16b, D11): a top
 * hairline, a close circle ([clearDescription], unticking everything) and
 * [count] ("3 sélectionnés", announced as it changes), then [actions]. With
 * [stacked] (Compact) the actions get a full-width row of their own under
 * the count, sharing it equally; otherwise they sit at the end of its row.
 */
@Composable
fun SelectionBar(
    count: String,
    clearDescription: String,
    onClear: () -> Unit,
    modifier: Modifier = Modifier,
    stacked: Boolean = false,
    actions: @Composable RowScope.() -> Unit,
) {
    Column(modifier = modifier.fillMaxWidth().background(AppColors.surface)) {
        AppDivider()
        Column(
            modifier = Modifier.padding(horizontal = AppSpacing.space5, vertical = AppSpacing.space3),
            verticalArrangement = Arrangement.spacedBy(AppSpacing.space3),
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(AppSpacing.space3),
            ) {
                AppCircleButton(onClick = onClear, contentDescription = clearDescription) {
                    CloseGlyph(modifier = Modifier.size(AppIconSize.sm), tint = AppColors.text)
                }
                Text(
                    text = count,
                    style = AppTypography.bodyEmphasis,
                    color = AppColors.text,
                    modifier = Modifier.weight(1f).semantics { liveRegion = LiveRegionMode.Polite },
                )
                if (!stacked) actions()
            }
            if (stacked) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(AppSpacing.space3),
                    verticalAlignment = Alignment.CenterVertically,
                    content = actions,
                )
            }
        }
    }
}
