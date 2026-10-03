package com.peltoche.fyde.theme

import androidx.compose.foundation.interaction.InteractionSource
import androidx.compose.foundation.interaction.collectIsFocusedAsState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.translate

/**
 * design-system.md § Color's "Focus ring: 2px `primary`, 2px outside the
 * element's own border... the one place `primary` is allowed to outline
 * rather than fill" — implemented here as a shared modifier rather than
 * per-component, since nothing in the component set had actually wired it
 * up and every caller needs the identical 2px-gap/2px-stroke geometry.
 *
 * [interactionSource] must be the same instance passed to the underlying
 * clickable/selectable control, so this draws only when *that* control has
 * real keyboard/accessibility focus, not on every composition. Draws
 * outside the component's own measured bounds (via [translate] on an
 * inflated [shape] outline) rather than reserving extra layout space, so
 * adding this never shifts surrounding content — it only ever adds a ring
 * on top when the element it's attached to is actually focused, which
 * matters most on desktop/web, where design-system.md's Expanded
 * breakpoint section already calls out keyboard navigation as a first-class
 * interaction, not just a touch fallback.
 */
@Composable
fun Modifier.appFocusRing(interactionSource: InteractionSource, shape: Shape = AppRadius.sm): Modifier {
    val isFocused by interactionSource.collectIsFocusedAsState()
    val color = AppColors.primary
    val strokeWidthDp = AppDimensions.borderFocus
    return this.drawWithContent {
        drawContent()
        if (isFocused) {
            val strokeWidthPx = strokeWidthDp.toPx()
            val gapPx = strokeWidthPx
            val outset = gapPx + strokeWidthPx / 2f
            val ringSize = Size(size.width + outset * 2f, size.height + outset * 2f)
            val outline = shape.createOutline(ringSize, layoutDirection, this)
            translate(left = -outset, top = -outset) {
                drawOutline(outline = outline, color = color, style = Stroke(width = strokeWidthPx))
            }
        }
    }
}
