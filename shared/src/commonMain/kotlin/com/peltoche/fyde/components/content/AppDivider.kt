package com.peltoche.fyde.components.content

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.width
import androidx.compose.material3.HorizontalDivider
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.Dp
import com.peltoche.fyde.theme.AppColors
import com.peltoche.fyde.theme.AppDimensions

/**
 * A hairline divider. [vertical] exists for [com.peltoche.fyde.layout.AppTwoPaneLayout]'s
 * pane separator — the one real side-by-side layout in the app — rather
 * than being a speculative variant; M3's own `HorizontalDivider` can't be
 * reoriented, so the vertical case draws a plain colored line instead.
 */
@Composable
fun AppDivider(modifier: Modifier = Modifier, thickness: Dp = AppDimensions.borderHairline, vertical: Boolean = false) {
    if (vertical) {
        Box(modifier = modifier.fillMaxHeight().width(thickness).background(AppColors.border))
    } else {
        HorizontalDivider(modifier = modifier, thickness = thickness, color = AppColors.border)
    }
}
