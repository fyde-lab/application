package com.peltoche.fyde.components.feedback

import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.size
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.unit.dp
import com.peltoche.fyde.theme.AppColors
import com.peltoche.fyde.theme.AppRadius
import com.peltoche.fyde.theme.AppSpacing
import com.peltoche.fyde.theme.AppTypography

/**
 * The full-screen/centered spinner — design-system.md's loading pattern for
 * when there's no content shape to preserve yet. When the shape *is* known
 * (a list about to load), prefer [AppSkeleton] blocks instead.
 */
@Composable
fun AppLoading(modifier: Modifier = Modifier, message: String? = null) {
    Box(modifier = modifier.fillMaxSize().background(AppColors.background), contentAlignment = Alignment.Center) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            CircularProgressIndicator(color = AppColors.primary, modifier = Modifier.size(36.dp))
            if (message != null) {
                Spacer(Modifier.height(AppSpacing.space4))
                Text(text = message, style = AppTypography.caption, color = AppColors.textMuted)
            }
        }
    }
}

/**
 * One pulsing placeholder block — the shape-preserving loading pattern,
 * currently duplicated as a one-off inside the document list. Composing
 * several of these (sized/shaped like the real content) replaces that
 * duplication once screens adopt it. Owns its pulse animation internally:
 * that's animation progress, not state any caller needs to observe.
 */
@Composable
fun AppSkeleton(modifier: Modifier = Modifier, shape: Shape = AppRadius.sm) {
    val transition = rememberInfiniteTransition(label = "skeleton")
    val alpha by transition.animateFloat(
        initialValue = 0.4f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(animation = tween(1000), repeatMode = RepeatMode.Reverse),
        label = "skeletonAlpha",
    )
    Box(modifier = modifier.clip(shape).background(AppColors.surfaceElevated.copy(alpha = alpha)))
}
