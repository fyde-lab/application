package com.peltoche.fyde.components.content

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.peltoche.fyde.theme.AppColors
import com.peltoche.fyde.theme.AppDimensions
import com.peltoche.fyde.theme.AppShapes
import com.peltoche.fyde.theme.AppSpacing
import com.peltoche.fyde.theme.appFocusRing

/**
 * design-system.md § Shape: no border by default (`surface`-vs-`background`
 * contrast already defines the edge); pass [outlined] only when two cards
 * of equal weight sit adjacent with no gap. Flat by design — never combine a
 * border with a shadow — so there's no elevation parameter to reach for.
 *
 * With [onClick] it's a control like any other: the keyboard focus ring
 * included. Found via review: script cards could be tabbed to, but showed
 * nothing but M3's faint state layer when focused.
 */
@Composable
fun AppCard(
    modifier: Modifier = Modifier,
    onClick: (() -> Unit)? = null,
    enabled: Boolean = true,
    outlined: Boolean = false,
    content: @Composable ColumnScope.() -> Unit,
) {
    val colors = CardDefaults.cardColors(containerColor = AppColors.surface, contentColor = AppColors.text)
    val elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
    val border = if (outlined) BorderStroke(AppDimensions.borderHairline, AppColors.border) else null
    val innerContent: @Composable ColumnScope.() -> Unit = {
        Column(modifier = Modifier.padding(AppSpacing.space5), content = content)
    }

    if (onClick != null) {
        val interactionSource = remember { MutableInteractionSource() }
        Card(
            onClick = onClick,
            modifier = modifier.appFocusRing(interactionSource, shape = AppShapes.card),
            enabled = enabled,
            interactionSource = interactionSource,
            shape = AppShapes.card,
            colors = colors,
            elevation = elevation,
            border = border,
            content = innerContent,
        )
    } else {
        Card(
            modifier = modifier,
            shape = AppShapes.card,
            colors = colors,
            elevation = elevation,
            border = border,
            content = innerContent,
        )
    }
}
