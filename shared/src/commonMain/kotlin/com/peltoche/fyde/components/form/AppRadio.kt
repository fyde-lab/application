package com.peltoche.fyde.components.form

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.peltoche.fyde.theme.AppColors

/** The radio mark's diameter — design-system.md § Components: 22pt marks. */
private val RadioSize: Dp = 22.dp

/**
 * A radio mark (`design/proposals/000-original-design/assets/mockups/tokens.css`'s `.radio`): a 1.5dp
 * `borderStrong` ring, or, [selected], a thick `primary` ring around a
 * `surface` dot. Only the mark — no click handling of its own: the row it
 * sits in is the target, through `Modifier.selectable` with
 * `Role.RadioButton` inside a `selectableGroup`, so a whole row is one
 * control instead of a mark and a label focused separately (as
 * [AppCheckbox] does with its label).
 */
@Composable
fun AppRadio(selected: Boolean, modifier: Modifier = Modifier, enabled: Boolean = true) {
    val ring = when {
        !enabled -> AppColors.border
        selected -> AppColors.primary
        else -> AppColors.borderStrong
    }
    val dot = AppColors.surface
    Canvas(modifier = modifier.size(RadioSize)) {
        val radius = size.minDimension / 2f
        if (selected) {
            drawCircle(color = ring, radius = radius)
            drawCircle(color = dot, radius = radius - 7.dp.toPx())
        } else {
            val stroke = 1.5.dp.toPx()
            drawCircle(color = ring, radius = radius - stroke / 2f, style = Stroke(width = stroke))
        }
    }
}
