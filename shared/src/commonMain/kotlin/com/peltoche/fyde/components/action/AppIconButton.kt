package com.peltoche.fyde.components.action

import androidx.compose.material3.IconButton
import androidx.compose.material3.IconButtonDefaults
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import com.peltoche.fyde.theme.AppColors

/**
 * An icon-only button. The app has no bundled icon pack yet (see the design
 * audit), so [content] takes whatever glyph the caller already has — a
 * hand-drawn `Canvas` shape today, a real `Icon` later — rather than this
 * component assuming a specific icon system.
 *
 * [contentDescription] is required, not optional: an icon-only control with
 * no accessible label is exactly the kind of thing screen readers can't do
 * anything with, so there's no default to silently fall back to.
 */
@Composable
fun AppIconButton(
    onClick: () -> Unit,
    contentDescription: String,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    tint: Color = AppColors.textMuted,
    content: @Composable () -> Unit,
) {
    IconButton(
        onClick = onClick,
        modifier = modifier.semantics { this.contentDescription = contentDescription },
        enabled = enabled,
        colors = IconButtonDefaults.iconButtonColors(
            contentColor = tint,
            disabledContentColor = AppColors.textMuted,
        ),
    ) {
        content()
    }
}
