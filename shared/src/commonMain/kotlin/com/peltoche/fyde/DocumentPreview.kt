package com.peltoche.fyde

import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.peltoche.fyde.components.feedback.AppEmptyState
import com.peltoche.fyde.components.feedback.AppErrorState
import com.peltoche.fyde.components.feedback.AppLoading
import com.peltoche.fyde.resources.Res
import com.peltoche.fyde.resources.preview_load_failed_description
import com.peltoche.fyde.resources.preview_load_failed_title
import com.peltoche.fyde.resources.preview_unavailable_description
import com.peltoche.fyde.resources.preview_unavailable_title
import com.peltoche.fyde.theme.AppColors
import com.peltoche.fyde.theme.AppIconSize
import com.peltoche.fyde.theme.AppSpacing
import com.peltoche.fyde.theme.AppTypography
import org.jetbrains.compose.resources.stringResource

/**
 * The document's content, previewed the way its [DocumentKind] allows: PDFs
 * through [PdfViewer], images decoded and drawn, anything else a plain "no
 * preview" state pointing at saving it. Listings carry no content, so a
 * previewable one is fetched through [loadContent] once shown, and again
 * only if its metadata changes.
 */
@Composable
internal fun DocumentPreview(
    document: FydeDocument,
    loadContent: suspend (FydeDocument) -> Result<ByteArray?>,
    modifier: Modifier = Modifier,
) {
    if (document.kind != DocumentKind.PDF && document.kind != DocumentKind.IMAGE) {
        PreviewUnavailable(document, modifier)
        return
    }
    // null while loading.
    val loaded by produceState<Result<ByteArray?>?>(null, document) { value = loadContent(document) }
    val content = loaded?.getOrNull()
    when {
        loaded == null -> AppLoading(modifier = modifier)
        content == null -> PreviewLoadFailed(modifier)
        document.kind == DocumentKind.PDF -> PdfViewer(pdfBytes = content, modifier = modifier)
        else -> {
            val image = remember(content) { decodeIconBitmap(content) }
            if (image != null) {
                Box(modifier = modifier.padding(horizontal = AppSpacing.space5), contentAlignment = Alignment.TopCenter) {
                    Image(bitmap = image, contentDescription = document.name, contentScale = ContentScale.Fit)
                }
            } else {
                PreviewUnavailable(document, modifier)
            }
        }
    }
}

@Composable
private fun PreviewLoadFailed(modifier: Modifier = Modifier) {
    AppErrorState(
        modifier = modifier,
        icon = { AlertGlyph(modifier = Modifier.size(AppIconSize.lg)) },
        title = stringResource(Res.string.preview_load_failed_title),
        description = stringResource(Res.string.preview_load_failed_description),
    )
}

@Composable
private fun PreviewUnavailable(document: FydeDocument, modifier: Modifier = Modifier) {
    AppEmptyState(
        modifier = modifier,
        icon = { DocumentGlyph(modifier = Modifier.size(AppIconSize.xl), tint = AppColors.textMuted, strokeWidth = 2.dp) },
        title = stringResource(Res.string.preview_unavailable_title),
        description = stringResource(Res.string.preview_unavailable_description, document.typeCode),
    )
}

/** One cell of an information grid (a script's saved parameters): a muted caption label stacked above the value. */
@Composable
internal fun DetailCell(label: String, value: String, modifier: Modifier = Modifier) {
    Column(modifier = modifier) {
        Text(text = label, style = AppTypography.caption, color = AppColors.textMuted)
        Spacer(Modifier.height(AppSpacing.space1))
        Text(
            text = value,
            style = AppTypography.body,
            color = AppColors.text,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
        )
    }
}
