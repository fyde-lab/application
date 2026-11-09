package com.peltoche.fyde.components.overlay

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.foundation.layout.size
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import com.peltoche.fyde.theme.AppColors
import com.peltoche.fyde.theme.AppIconSize
import com.peltoche.fyde.theme.AppShapes
import com.peltoche.fyde.theme.AppTypography

/**
 * A popover menu (design-system.md § Elevation, level 3): `surface-elevated`
 * with M3's own small shadow, and `radius-lg` like every other floating
 * surface. Found via review: the app's three menus (filter chips, the
 * language picker, [com.peltoche.fyde.components.action.AppMenuButton]) were
 * each styled by hand or not at all — two kept M3's 2dp corners and its
 * baseline container color.
 */
@Composable
fun AppDropdownMenu(
    expanded: Boolean,
    onDismissRequest: () -> Unit,
    modifier: Modifier = Modifier,
    content: @Composable ColumnScope.() -> Unit,
) {
    DropdownMenu(
        expanded = expanded,
        onDismissRequest = onDismissRequest,
        modifier = modifier,
        shape = AppShapes.popover,
        containerColor = AppColors.surfaceElevated,
        content = content,
    )
}

/**
 * One [AppDropdownMenu] entry, its label in the Body role. [destructive]
 * colors it and its [leadingIcon] `error`, for an action that can't be
 * undone. [shortcut] ("⌘⌫") ends it in `caption`, unless [trailingContent]
 * takes that place.
 */
@Composable
fun AppDropdownMenuItem(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    destructive: Boolean = false,
    leadingIcon: (@Composable (tint: Color) -> Unit)? = null,
    shortcut: String? = null,
    trailingContent: (@Composable () -> Unit)? = null,
) {
    val color = if (destructive) AppColors.error else AppColors.text
    DropdownMenuItem(
        text = { Text(text, style = AppTypography.body, color = color) },
        onClick = onClick,
        modifier = modifier,
        leadingIcon = leadingIcon?.let { icon -> { Box(Modifier.size(AppIconSize.md)) { icon(color) } } },
        trailingIcon = trailingContent ?: shortcut?.let { { Text(it, style = AppTypography.caption, color = AppColors.textMuted) } },
    )
}
