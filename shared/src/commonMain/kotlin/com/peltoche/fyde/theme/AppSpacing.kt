package com.peltoche.fyde.theme

import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

/**
 * The 4dp-based spacing scale from `design/current/design-system.md` § Spacing.
 * Identical in both themes and on every platform, so this is a plain object
 * rather than a `CompositionLocal` — nothing about it varies at runtime.
 */
object AppSpacing {
    val space1: Dp = 2.dp
    val space2: Dp = 4.dp
    val space3: Dp = 8.dp
    val space4: Dp = 12.dp
    val space5: Dp = 16.dp
    val space6: Dp = 20.dp
    val space7: Dp = 24.dp
    val space8: Dp = 32.dp
    val space9: Dp = 40.dp
    val space10: Dp = 56.dp
}
