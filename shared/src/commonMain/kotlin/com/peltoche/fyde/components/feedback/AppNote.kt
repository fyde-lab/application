package com.peltoche.fyde.components.feedback

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.semantics.LiveRegionMode
import androidx.compose.ui.semantics.liveRegion
import androidx.compose.ui.semantics.semantics
import com.peltoche.fyde.theme.AppColors
import com.peltoche.fyde.theme.AppIconSize
import com.peltoche.fyde.theme.AppShapes
import com.peltoche.fyde.theme.AppSpacing
import com.peltoche.fyde.theme.AppTypography

/** What an [AppNote] says about the condition it explains — design-system.md § Patterns. */
enum class AppNoteKind { Info, Ok, Warning, Error }

/**
 * A note explaining a condition inline, next to what it concerns
 * (design-system.md § Patterns: "there is no full-width banner"): its
 * [kind]'s wash, an [icon] in the kind's colour, then [message]. The icon
 * is handed that colour, since the app's glyphs don't read
 * `LocalContentColor`. Announced politely when it appears.
 *
 * Replaces [AppBanner] for inline conditions; screens move over as they're
 * rebuilt to their reference screen.
 */
@Composable
fun AppNote(
    message: String,
    kind: AppNoteKind,
    icon: @Composable (tint: Color) -> Unit,
    modifier: Modifier = Modifier,
) {
    val (wash, accent) = when (kind) {
        AppNoteKind.Info -> MaterialTheme.colorScheme.surfaceVariant to AppColors.textMuted
        AppNoteKind.Ok -> AppColors.tintOf(AppColors.primary) to AppColors.success
        AppNoteKind.Warning -> AppColors.tintOf(AppColors.warning) to AppColors.warning
        AppNoteKind.Error -> AppColors.tintOf(AppColors.error) to AppColors.error
    }
    Row(
        modifier = modifier
            .fillMaxWidth()
            .clip(AppShapes.note)
            .background(wash)
            .padding(AppSpacing.space4)
            .semantics { liveRegion = LiveRegionMode.Polite },
        horizontalArrangement = Arrangement.spacedBy(AppSpacing.space3),
        verticalAlignment = Alignment.Top,
    ) {
        Box(modifier = Modifier.size(AppIconSize.md), contentAlignment = Alignment.Center) { icon(accent) }
        Text(text = message, style = AppTypography.body, color = AppColors.text, modifier = Modifier.weight(1f))
    }
}
