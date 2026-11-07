package com.peltoche.fyde.components.content

import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.peltoche.fyde.theme.AppColors
import com.peltoche.fyde.theme.AppSpacing
import com.peltoche.fyde.theme.AppTypography

/**
 * A section's label (Settings, Sources, the vault's latest arrivals), in the
 * Section role above the group it heads, spaced from the group before it —
 * unless it's the [first] on its page, right under the page's title.
 */
@Composable
fun AppSectionTitle(text: String, modifier: Modifier = Modifier, first: Boolean = false) {
    Text(
        text = text,
        style = AppTypography.section,
        color = AppColors.text,
        modifier = modifier.padding(top = if (first) 0.dp else AppSpacing.space7, bottom = AppSpacing.space3),
    )
}
