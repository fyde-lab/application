package com.peltoche.fyde.components.content

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import com.peltoche.fyde.theme.AppColors
import com.peltoche.fyde.theme.AppRadius
import com.peltoche.fyde.theme.AppSpacing
import com.peltoche.fyde.theme.AppTypography

/**
 * A similar document's ressemblance in a list row (V6c, V16, D11): [text]
 * ("94 %") in `data` on a `sunk` pill, read out as [description]
 * ("ressemblance 94 %"). The caller formats the SDK's confidence.
 */
@Composable
fun SimilarityBadge(text: String, description: String, modifier: Modifier = Modifier) {
    Text(
        text = text,
        style = AppTypography.data,
        color = AppColors.text,
        maxLines = 1,
        modifier = modifier
            .clip(AppRadius.full)
            // `sunk` until the tokens migrate.
            .background(AppColors.surfaceElevated)
            .padding(horizontal = AppSpacing.space3, vertical = AppSpacing.space1)
            .semantics { contentDescription = description },
    )
}
