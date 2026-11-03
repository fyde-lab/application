package com.peltoche.fyde.components.content

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material3.Text
import androidx.compose.material3.ripple
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import com.peltoche.fyde.components.feedback.AppSkeleton
import com.peltoche.fyde.theme.AppCategoryColor
import com.peltoche.fyde.theme.AppColors
import com.peltoche.fyde.theme.AppDimensions
import com.peltoche.fyde.theme.AppIconSize
import com.peltoche.fyde.theme.AppShapes
import com.peltoche.fyde.theme.AppSpacing
import com.peltoche.fyde.theme.AppTypography
import com.peltoche.fyde.theme.appFocusRing

/**
 * A category on the vault's home (V1, D6): its [icon] and [count] on top, its
 * [name] and [caption] (the newest document in it) below, on the category's
 * fill. Read out as one control, [contentDescription] standing for all four.
 */
@Composable
fun CategoryCard(
    name: String,
    count: String,
    caption: String?,
    color: AppCategoryColor,
    contentDescription: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    icon: @Composable (tint: Color) -> Unit,
) {
    val interactionSource = remember { MutableInteractionSource() }
    Column(
        modifier = modifier
            .height(AppDimensions.categoryCardHeight)
            .clip(AppShapes.card)
            .background(color.fill)
            .clickable(interactionSource = interactionSource, indication = ripple(), onClick = onClick)
            .appFocusRing(interactionSource, shape = AppShapes.card)
            .clearAndSetSemantics {
                this.contentDescription = contentDescription
                role = Role.Button
            }
            .padding(AppSpacing.space4),
        verticalArrangement = Arrangement.SpaceBetween,
    ) {
        Row(modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
            CategoryIconTile(size = AppDimensions.categoryCardIcon) { icon(color.ink) }
            Spacer(Modifier.weight(1f))
            Text(text = count, style = AppTypography.bodyEmphasis, color = color.ink, maxLines = 1)
        }
        Column {
            Text(text = name, style = AppTypography.section, color = AppColors.text, maxLines = 1, overflow = TextOverflow.Ellipsis)
            if (caption != null) {
                Text(text = caption, style = AppTypography.caption, color = AppColors.categoryCaption, maxLines = 1, overflow = TextOverflow.Ellipsis)
            }
        }
    }
}

/** A [CategoryCard]'s place while the vault's summary loads. */
@Composable
fun CategoryCardPlaceholder(modifier: Modifier = Modifier) {
    AppSkeleton(modifier = modifier.fillMaxWidth().height(AppDimensions.categoryCardHeight), shape = AppShapes.card)
}

/**
 * The head of a category page (V3): the category's icon, [name] and
 * [subtitle] ("38 documents") on its fill — the page's heading.
 */
@Composable
fun CategoryBand(
    name: String,
    subtitle: String?,
    color: AppCategoryColor,
    modifier: Modifier = Modifier,
    icon: @Composable (tint: Color) -> Unit,
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .clip(AppShapes.card)
            .background(color.fill)
            .padding(AppSpacing.space5),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        CategoryIconTile(size = AppDimensions.iconTileLarge) { icon(color.ink) }
        Spacer(Modifier.width(AppSpacing.space4))
        Column {
            Text(
                text = name,
                style = AppTypography.title,
                color = AppColors.text,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.semantics { heading() },
            )
            if (subtitle != null) {
                Text(text = subtitle, style = AppTypography.caption, color = AppColors.categoryCaption, maxLines = 1)
            }
        }
    }
}

/**
 * The pale square a category's icon sits in, on that category's fill — the
 * card's and the band's. [content] draws the icon in the category's ink,
 * stretched to [AppIconSize.md].
 */
@Composable
fun CategoryIconTile(size: Dp, modifier: Modifier = Modifier, content: @Composable () -> Unit) {
    Box(
        modifier = modifier.size(size).clip(AppShapes.iconTile).background(AppColors.categoryTile),
        contentAlignment = Alignment.Center,
    ) {
        Box(Modifier.size(AppIconSize.md), propagateMinConstraints = true) { content() }
    }
}
