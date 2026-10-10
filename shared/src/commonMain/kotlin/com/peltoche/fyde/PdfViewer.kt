package com.peltoche.fyde

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.unit.dp
import com.peltoche.fyde.components.feedback.AppErrorState
import com.peltoche.fyde.resources.Res
import com.peltoche.fyde.resources.pdf_render_failed_description
import com.peltoche.fyde.resources.pdf_render_failed_title
import com.peltoche.fyde.theme.AppColors
import com.peltoche.fyde.theme.AppSpacing
import org.jetbrains.compose.resources.stringResource

/**
 * Renders [pdfBytes] (a whole PDF file, already decrypted by `fyde-sdk`) as a
 * vertically scrollable stack of pages, using each platform's own PDF engine
 * (Android's `PdfRenderer`, iOS's PDFKit, JVM's PDFBox) so no PDF-parsing
 * library needs to be shared across targets. Not yet implemented for Web,
 * see `../../../CLAUDE.md`.
 *
 * Pages are rendered lazily, one at a time as they scroll into view, through
 * [PdfPage] — found via review that rendering every page up front kept a
 * long PDF behind a spinner, and every page's bitmap in memory, before the
 * first one could be seen. Loading/failure states use the app's own
 * components ([com.peltoche.fyde.components.feedback.AppLoading],
 * [PdfRenderFailed]) rather than a raw spinner and a red exception message.
 */
@Composable
expect fun PdfViewer(pdfBytes: ByteArray, modifier: Modifier = Modifier)

/** Height/width of an A4 page, the placeholder shape a page takes until it's rendered. */
private const val PAGE_PLACEHOLDER_ASPECT_RATIO = 1f / 1.414f

/** One page of a [PdfViewer]: [page] once rendered, a blank page-shaped placeholder until then. */
@Composable
internal fun PdfPage(page: ImageBitmap?) {
    val modifier = Modifier.fillMaxWidth().padding(vertical = AppSpacing.space1)
    if (page == null) {
        Box(modifier.aspectRatio(PAGE_PLACEHOLDER_ASPECT_RATIO).background(AppColors.surface))
    } else {
        Image(bitmap = page, contentDescription = null, contentScale = ContentScale.FillWidth, modifier = modifier)
    }
}

/**
 * Shown when [PdfViewer]'s engine can't open the file at all. Never shows the
 * engine's own exception message — that's technical, not wording a user can
 * act on — and points at the one thing they can still do: download it.
 */
@Composable
internal fun PdfRenderFailed(modifier: Modifier = Modifier) {
    AppErrorState(
        modifier = modifier,
        icon = { AlertGlyph(modifier = Modifier.size(32.dp)) },
        title = stringResource(Res.string.pdf_render_failed_title),
        description = stringResource(Res.string.pdf_render_failed_description),
    )
}
