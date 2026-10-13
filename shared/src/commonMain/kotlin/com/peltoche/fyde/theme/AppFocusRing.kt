package com.peltoche.fyde.theme

import androidx.compose.foundation.interaction.FocusInteraction
import androidx.compose.foundation.interaction.InteractionSource
import androidx.compose.foundation.interaction.PressInteraction
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.drawOutline
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.translate
import kotlin.time.Duration.Companion.milliseconds
import kotlin.time.TimeSource

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
 *
 * Only for focus that came from the keyboard (Tab, arrow keys), the way a
 * browser's `:focus-visible` works: focus that a pointer press gave the
 * control draws no ring. Found via review: clicking a list row or a filter
 * chip left a ring around it, which on a selected row also covered its
 * selection accent bar. Compose has no focus-visible notion of its own, so
 * focus counts as the pointer's when it lands within [POINTER_FOCUS_WINDOW]
 * of a press on the same control, in either order (a pointer may focus on
 * down, before the press is reported). A press long after focus — Enter on
 * a control reached with Tab — leaves the ring as is.
 */
@Composable
fun Modifier.appFocusRing(interactionSource: InteractionSource, shape: Shape = AppRadius.sm): Modifier {
    var isFocused by remember(interactionSource) { mutableStateOf(false) }
    var pointerFocused by remember(interactionSource) { mutableStateOf(false) }
    LaunchedEffect(interactionSource) {
        var lastPress: TimeSource.Monotonic.ValueTimeMark? = null
        var lastFocus: TimeSource.Monotonic.ValueTimeMark? = null
        fun recent(mark: TimeSource.Monotonic.ValueTimeMark?) = mark != null && mark.elapsedNow() < POINTER_FOCUS_WINDOW
        interactionSource.interactions.collect { interaction ->
            when (interaction) {
                is FocusInteraction.Focus -> {
                    isFocused = true
                    pointerFocused = recent(lastPress)
                    lastFocus = TimeSource.Monotonic.markNow()
                }
                is FocusInteraction.Unfocus -> {
                    isFocused = false
                    pointerFocused = false
                }
                is PressInteraction.Press -> {
                    if (isFocused && recent(lastFocus)) pointerFocused = true
                    lastPress = TimeSource.Monotonic.markNow()
                }
            }
        }
    }
    val showRing = isFocused && !pointerFocused
    val color = AppColors.primary
    val strokeWidthDp = AppDimensions.borderFocus
    return this.drawWithContent {
        drawContent()
        if (showRing) {
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

/** How close a press and a focus change must be for the focus to count as the pointer's; see [appFocusRing]. */
private val POINTER_FOCUS_WINDOW = 300.milliseconds
