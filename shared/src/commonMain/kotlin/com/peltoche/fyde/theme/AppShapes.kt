package com.peltoche.fyde.theme

import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Shapes
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.unit.dp

/**
 * The corner-radius scale from `design/design-system.md` § Shape — small on
 * purpose: precise and geometric rather than the soft/pill rounding generic
 * SaaS UIs default to. Nothing here is a pill except [full], which is
 * reserved for genuine circles.
 */
object AppRadius {
    val none: Shape = RoundedCornerShape(0.dp)
    val xs: Shape = RoundedCornerShape(2.dp)
    val sm: Shape = RoundedCornerShape(4.dp)
    val md: Shape = RoundedCornerShape(6.dp)
    val lg: Shape = RoundedCornerShape(10.dp)
    val full: Shape = CircleShape
}

/** Named per-component shape assignments, per design-system.md § Shape. */
object AppShapes {
    val button: Shape = AppRadius.md
    val input: Shape = AppRadius.sm
    val card: Shape = AppRadius.md
    val dialog: Shape = AppRadius.lg
}

/**
 * The M3 [Shapes] slots, for stock Material components that read shape from
 * `MaterialTheme.shapes` rather than from [AppShapes] directly (e.g. a
 * default `Card` or `AlertDialog`). `extraLarge` has no distinct value in
 * the spec beyond `radius-lg` — the scale deliberately stops growing there
 * rather than ballooning for rare, very-large containers.
 */
fun appShapes(): Shapes = Shapes(
    extraSmall = RoundedCornerShape(2.dp),
    small = RoundedCornerShape(4.dp),
    medium = RoundedCornerShape(6.dp),
    large = RoundedCornerShape(10.dp),
    extraLarge = RoundedCornerShape(10.dp),
)
