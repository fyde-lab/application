package com.peltoche.fyde.components.content

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import com.peltoche.fyde.theme.AppColors
import com.peltoche.fyde.theme.AppRadius
import com.peltoche.fyde.theme.AppSpacing
import com.peltoche.fyde.theme.AppTypography

/** Semantic emphasis for [AppBadge] — kept separate from the brand accent, per design-system.md's "semantic color is separate from the accent" principle. */
enum class AppBadgeVariant { Neutral, Primary, Success, Warning, Error }

/** A small status/count label. Background is the variant color's tinted container ([AppColors.tintOf]). */
@Composable
fun AppBadge(
    text: String,
    modifier: Modifier = Modifier,
    variant: AppBadgeVariant = AppBadgeVariant.Neutral,
) {
    val color = when (variant) {
        AppBadgeVariant.Neutral -> AppColors.textMuted
        AppBadgeVariant.Primary -> AppColors.primary
        AppBadgeVariant.Success -> AppColors.success
        AppBadgeVariant.Warning -> AppColors.warning
        AppBadgeVariant.Error -> AppColors.error
    }
    Text(
        text = text,
        style = AppTypography.micro,
        color = color,
        modifier = modifier
            .clip(AppRadius.xs)
            .background(AppColors.tintOf(color))
            .padding(horizontal = AppSpacing.space3, vertical = AppSpacing.space1),
    )
}
