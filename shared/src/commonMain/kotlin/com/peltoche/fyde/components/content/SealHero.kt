package com.peltoche.fyde.components.content

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import com.peltoche.fyde.icons.LockGlyph
import com.peltoche.fyde.theme.AppColors
import com.peltoche.fyde.theme.AppDimensions

/**
 * The seal — design-system.md's one illustration ("no illustrations beyond
 * the seal"): a padlock on a `primary` disc, inside a wash of `primary` and
 * two `border` hairline rings. Heads Welcome (O1), and Sécurité (M2) once
 * it exists.
 *
 * Proportions come from `design/proposals/000-original-design/assets/mockups/onboarding.html`'s `.hero`,
 * on its [AppDimensions.sealHero] diameter of 168: a 116 wash, rings at
 * radius 72 and 84, an 84 disc holding a 38 lock.
 *
 * [onPrimary] draws it on a `primary` fill instead — the desktop brand panel
 * (DO1, `design/proposals/000-original-design/assets/mockups/desktop-auth.html`'s `.seal`): an `onPrimary`
 * disc holding a `primary` lock, an `onPrimary` wash, and rings at 22% and
 * 16% `onPrimary`.
 */
@Composable
fun SealHero(modifier: Modifier = Modifier, contentDescription: String? = null, onPrimary: Boolean = false) {
    val wash = if (onPrimary) AppColors.onPrimary.copy(alpha = 0.12f) else AppColors.tintOf(AppColors.primary)
    val innerRing = if (onPrimary) AppColors.onPrimary.copy(alpha = 0.22f) else AppColors.border
    val outerRing = if (onPrimary) AppColors.onPrimary.copy(alpha = 0.16f) else AppColors.border
    val disc = if (onPrimary) AppColors.onPrimary else AppColors.primary
    val lock = if (onPrimary) AppColors.primary else AppColors.onPrimary
    Box(
        modifier = modifier
            .size(AppDimensions.sealHero)
            .then(if (contentDescription != null) Modifier.semantics { this.contentDescription = contentDescription } else Modifier),
        contentAlignment = Alignment.Center,
    ) {
        Canvas(modifier = Modifier.fillMaxSize()) {
            val unit = size.minDimension / 168f
            val hairline = Stroke(width = AppDimensions.borderHairline.toPx())
            drawCircle(color = outerRing, radius = 83.5f * unit, style = hairline)
            drawCircle(color = innerRing, radius = 71.5f * unit, style = hairline)
            drawCircle(color = wash, radius = 58f * unit)
        }
        Box(
            modifier = Modifier
                .size(84.dp)
                .clip(CircleShape)
                .background(disc),
            contentAlignment = Alignment.Center,
        ) {
            // The icon set's 1.7 stroke on its 24-unit grid, drawn at 38.
            LockGlyph(modifier = Modifier.size(38.dp), tint = lock, strokeWidth = 2.7.dp)
        }
    }
}
