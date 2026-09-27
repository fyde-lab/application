package com.peltoche.fyde.components.content

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.peltoche.fyde.theme.AppColors
import com.peltoche.fyde.theme.AppDimensions
import com.peltoche.fyde.theme.AppShapes
import com.peltoche.fyde.theme.AppSpacing

/**
 * design-system.md § Shape: no border by default (`surface`-vs-`background`
 * contrast already defines the edge); pass [outlined] only when two cards
 * of equal weight sit adjacent with no gap. Flat by design — never combine a
 * border with a shadow — so there's no elevation parameter to reach for.
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
        Card(
            onClick = onClick,
            modifier = modifier,
            enabled = enabled,
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
