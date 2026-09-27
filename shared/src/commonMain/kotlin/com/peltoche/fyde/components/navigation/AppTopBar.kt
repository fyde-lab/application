package com.peltoche.fyde.components.navigation

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.peltoche.fyde.components.action.AppIconButton
import com.peltoche.fyde.components.content.AppDivider
import com.peltoche.fyde.layout.AppWindowSizeClass
import com.peltoche.fyde.layout.LocalAppWindowSizeClass
import com.peltoche.fyde.theme.AppColors
import com.peltoche.fyde.theme.AppDimensions
import com.peltoche.fyde.theme.AppSpacing
import com.peltoche.fyde.theme.AppTypography

/**
 * Generalizes the top-bar shape already duplicated across three screens
 * (document list, PDF viewer, settings). Title size follows the precedent
 * already set across those screens: [AppTypography.title] for a top-level
 * screen with no back button, [AppTypography.section] once there's a back
 * affordance — baked in here rather than exposed as a parameter callers
 * would have to remember to set correctly themselves.
 *
 * Height and leading edge both grow at Medium/Expanded — design-system.md's
 * carried-forward Navigation guidance — rather than staying a fixed mobile
 * size everywhere.
 */
@Composable
fun AppTopBar(
    title: String,
    modifier: Modifier = Modifier,
    onBack: (() -> Unit)? = null,
    backContentDescription: String? = null,
    windowSizeClass: AppWindowSizeClass = LocalAppWindowSizeClass.current,
    actions: @Composable RowScope.() -> Unit = {},
) {
    val height = if (windowSizeClass == AppWindowSizeClass.Compact) AppDimensions.topBarHeight else AppDimensions.topBarHeightExpanded
    val leadingEdge = when {
        onBack != null -> AppSpacing.space2
        windowSizeClass == AppWindowSizeClass.Compact -> AppSpacing.space5
        windowSizeClass == AppWindowSizeClass.Medium -> AppSpacing.space7
        else -> AppSpacing.space8
    }

    Box(
        modifier = modifier
            .fillMaxWidth()
            .height(height)
            .background(AppColors.surfaceElevated),
        contentAlignment = Alignment.CenterStart,
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(start = leadingEdge, end = AppSpacing.space3),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            if (onBack != null) {
                // "Back" is a last-resort fallback for a caller that forgot
                // to pass one, never the intended copy source — pass a real
                // localized string via backContentDescription instead.
                AppIconButton(onClick = onBack, contentDescription = backContentDescription ?: "Back") {
                    BackChevron(tint = AppColors.text)
                }
            }
            Text(
                text = title,
                style = if (onBack != null) AppTypography.section else AppTypography.title,
                color = AppColors.text,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.weight(1f),
            )
            actions()
        }
        AppDivider(modifier = Modifier.align(Alignment.BottomCenter))
    }
}

@Composable
private fun BackChevron(tint: Color, modifier: Modifier = Modifier) {
    Canvas(modifier = modifier.size(16.dp)) {
        val strokeWidth = 1.6.dp.toPx()
        val path = Path().apply {
            moveTo(size.width * 0.7f, 0f)
            lineTo(size.width * 0.15f, size.height / 2f)
            lineTo(size.width * 0.7f, size.height)
        }
        drawPath(
            path,
            color = tint,
            style = Stroke(width = strokeWidth, cap = StrokeCap.Round, join = StrokeJoin.Round),
        )
    }
}
