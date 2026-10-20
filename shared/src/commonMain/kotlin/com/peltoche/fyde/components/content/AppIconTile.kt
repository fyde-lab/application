package com.peltoche.fyde.components.content

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.Dp
import com.peltoche.fyde.theme.AppColors
import com.peltoche.fyde.theme.AppDimensions
import com.peltoche.fyde.theme.AppShapes

/**
 * The square leading visual of a list row, card or detail header — a
 * document's file kind, a script's logo — with its [content] centered.
 *
 * With a [tint], the tile is that color's tinted container
 * ([AppColors.tintOf]), for a glyph the app colors itself to code a kind or
 * state. Without one it's neutral (page `background` + `border` hairline),
 * for content that brings its own colors: third-party logos on a teal wash
 * clashed, and spent the one accent on decoration rather than emphasis.
 *
 * A loading placeholder for a tile is an `AppSkeleton` of the same size,
 * shaped [AppShapes.iconTile].
 */
@Composable
fun AppIconTile(
    modifier: Modifier = Modifier,
    size: Dp = AppDimensions.iconTile,
    tint: Color? = null,
    content: @Composable BoxScope.() -> Unit,
) {
    val container = modifier.size(size).clip(AppShapes.iconTile)
    Box(
        modifier = if (tint != null) {
            container.background(AppColors.tintOf(tint))
        } else {
            container
                .background(AppColors.background)
                .border(AppDimensions.borderHairline, AppColors.border, AppShapes.iconTile)
        },
        contentAlignment = Alignment.Center,
        content = content,
    )
}
