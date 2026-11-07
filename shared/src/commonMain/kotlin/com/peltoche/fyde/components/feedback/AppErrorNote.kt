package com.peltoche.fyde.components.feedback

import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import com.peltoche.fyde.icons.AlertGlyph
import com.peltoche.fyde.theme.AppIconSize

/**
 * Why a page couldn't load or an action failed, inline where it applies
 * (design-system.md: no banners): an [AppNote] of kind
 * [AppNoteKind.Error] with the alert glyph.
 */
@Composable
fun AppErrorNote(message: String) {
    AppNote(
        message = message,
        kind = AppNoteKind.Error,
        icon = { tint -> AlertGlyph(modifier = Modifier.size(AppIconSize.sm), tint = tint) },
    )
}
