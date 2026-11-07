package com.peltoche.fyde

import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.peltoche.fyde.components.feedback.AppEmptyState
import com.peltoche.fyde.icons.DocumentGlyph
import com.peltoche.fyde.resources.Res
import com.peltoche.fyde.resources.pdf_preview_unavailable
import com.peltoche.fyde.resources.preview_unavailable_title
import com.peltoche.fyde.theme.AppColors
import org.jetbrains.compose.resources.stringResource

/** `fyde-sdk` isn't linked into the Web target (see `FydeSdk.web.kt`), so there's no PDF engine to render with yet. */
@Composable
actual fun PdfViewer(pdfBytes: ByteArray, modifier: Modifier) {
    AppEmptyState(
        modifier = modifier,
        icon = { DocumentGlyph(modifier = Modifier.size(40.dp), tint = AppColors.textMuted, strokeWidth = 2.dp) },
        title = stringResource(Res.string.preview_unavailable_title),
        description = stringResource(Res.string.pdf_preview_unavailable),
    )
}
