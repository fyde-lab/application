package com.peltoche.fyde.components.content

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import com.peltoche.fyde.theme.AppColors
import com.peltoche.fyde.theme.AppDimensions
import com.peltoche.fyde.theme.AppRadius
import com.peltoche.fyde.theme.AppSpacing

/**
 * A `surface` group with a hairline border (`tokens.css`'s `.group`): the
 * card the app's lists, settings and facts sit in. Its rows
 * ([AppGroupRow]) add their own [AppDivider]s.
 *
 * Not [AppCard], which draws the design system's previous card (6dp radius,
 * no border): the two meet when the tokens migrate (design-system.md
 * § Status).
 */
@Composable
fun AppGroup(modifier: Modifier = Modifier, content: @Composable ColumnScope.() -> Unit) {
    Column(
        modifier = modifier
            .fillMaxWidth()
            .clip(AppRadius.lg)
            .background(AppColors.surface)
            .border(AppDimensions.borderHairline, AppColors.border, AppRadius.lg),
        content = content,
    )
}

/** An [AppGroup] with the card's inner padding, for content that isn't rows. */
@Composable
fun AppGroupCard(modifier: Modifier = Modifier, content: @Composable ColumnScope.() -> Unit) {
    AppGroup(modifier = modifier) {
        Column(modifier = Modifier.fillMaxWidth().padding(AppSpacing.space5), content = content)
    }
}
