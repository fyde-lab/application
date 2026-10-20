package com.peltoche.fyde.components.navigation

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FilledIconButton
import androidx.compose.material3.IconButtonDefaults
import androidx.compose.material3.LocalContentColor
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.peltoche.fyde.components.action.AppButton
import com.peltoche.fyde.components.action.AppIconButton
import com.peltoche.fyde.components.content.AppDivider
import com.peltoche.fyde.layout.AppContentWidth
import com.peltoche.fyde.layout.AppWindowSizeClass
import com.peltoche.fyde.layout.LocalAppWindowSizeClass
import com.peltoche.fyde.layout.contentMaxWidth
import com.peltoche.fyde.layout.screenMargin
import com.peltoche.fyde.resources.Res
import com.peltoche.fyde.resources.back
import com.peltoche.fyde.theme.AppColors
import com.peltoche.fyde.theme.AppDimensions
import com.peltoche.fyde.theme.AppShapes
import com.peltoche.fyde.theme.AppTypography
import com.peltoche.fyde.theme.appFocusRing
import org.jetbrains.compose.resources.stringResource

/**
 * Generalizes the top-bar shape already duplicated across three screens
 * (document list, PDF viewer, settings). Title size follows the precedent
 * already set across those screens: [AppTypography.title] for a top-level
 * screen with no back button, [AppTypography.section] once there's a back
 * affordance — baked in here rather than exposed as a parameter callers
 * would have to remember to set correctly themselves.
 *
 * Height grows at Medium/Expanded — design-system.md's carried-forward
 * Navigation guidance — rather than staying a fixed mobile size everywhere.
 *
 * Its edges are the content's: [width] means what it means for the
 * screen's `AppContentContainer` (pass the same value), so the title starts
 * where the content does and [actions] end where it ends. A back chevron is
 * pulled out by its button's inset, so the chevron itself sits on that
 * edge. Found via review: the title used to sit 16dp right of the content
 * edge, and actions ended 8dp from the window edge against the content's
 * 32dp margin.
 *
 * At Expanded it sits under `TopLevelScaffold`'s navigation bar, so it's
 * drawn flat — no fill, no divider — as the page's heading rather than a
 * second bar stacked on the first.
 */
@Composable
fun AppTopBar(
    title: String,
    modifier: Modifier = Modifier,
    onBack: (() -> Unit)? = null,
    backContentDescription: String? = null,
    windowSizeClass: AppWindowSizeClass = LocalAppWindowSizeClass.current,
    width: AppContentWidth = AppContentWidth.List,
    actions: @Composable RowScope.() -> Unit = {},
) {
    val height = if (windowSizeClass == AppWindowSizeClass.Compact) AppDimensions.topBarHeight else AppDimensions.topBarHeightExpanded
    val contentStart = screenMargin(windowSizeClass)
    val flat = windowSizeClass == AppWindowSizeClass.Expanded
    // An icon button centers its 16dp chevron in a 48dp target: 16dp inset.
    val leadingEdge = if (onBack != null) (contentStart - BackChevronInset).coerceAtLeast(0.dp) else contentStart

    Box(
        modifier = modifier
            .fillMaxWidth()
            .height(height)
            .background(if (flat) AppColors.background else AppColors.surfaceElevated),
        contentAlignment = Alignment.Center,
    ) {
        Row(
            modifier = Modifier
                .widthIn(max = contentMaxWidth(width))
                .fillMaxWidth()
                .padding(start = leadingEdge, end = screenMargin(windowSizeClass)),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            if (onBack != null) {
                // Localized like every other string: found via review that a
                // caller passing nothing got a hardcoded English "Back".
                AppIconButton(onClick = onBack, contentDescription = backContentDescription ?: stringResource(Res.string.back)) {
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
        if (!flat) AppDivider(modifier = Modifier.align(Alignment.BottomCenter))
    }
}

/**
 * A screen's one primary action, for [AppTopBar]'s `actions` — the same
 * component on every screen that has one (Upload document), so
 * the same kind of action never looks different from one destination to the
 * next. Found via review: Documents had a filled Upload button and Scripts a
 * text-only New script link in the very same slot.
 *
 * Filled, with [icon] and [text], from Medium up. On Compact it's the
 * filled [icon] alone, labelled with [text] for assistive tech: found via
 * review that the full "Upload document" button took half a phone's top
 * bar. [icon] is handed the color to draw itself in, since the app's glyphs
 * don't read `LocalContentColor`.
 */
@Composable
fun AppTopBarAction(
    text: String,
    onClick: () -> Unit,
    icon: @Composable (tint: Color) -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    loading: Boolean = false,
    windowSizeClass: AppWindowSizeClass = LocalAppWindowSizeClass.current,
) {
    if (windowSizeClass != AppWindowSizeClass.Compact) {
        AppButton(
            text = text,
            onClick = onClick,
            modifier = modifier,
            enabled = enabled,
            loading = loading,
            leadingIcon = { icon(AppColors.onPrimary) },
        )
        return
    }
    val interactionSource = remember { MutableInteractionSource() }
    FilledIconButton(
        onClick = onClick,
        modifier = modifier
            .appFocusRing(interactionSource, shape = AppShapes.button)
            .semantics { contentDescription = text },
        enabled = enabled && !loading,
        shape = AppShapes.button,
        colors = IconButtonDefaults.filledIconButtonColors(
            containerColor = AppColors.primary,
            contentColor = AppColors.onPrimary,
        ),
        interactionSource = interactionSource,
    ) {
        if (loading) {
            CircularProgressIndicator(modifier = Modifier.size(18.dp), strokeWidth = 2.dp, color = LocalContentColor.current)
        } else {
            icon(AppColors.onPrimary)
        }
    }
}

private val BackChevronInset = 16.dp

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
