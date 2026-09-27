package com.peltoche.fyde

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import com.peltoche.fyde.resources.Res
import com.peltoche.fyde.resources.back
import com.peltoche.fyde.theme.AppColors
import com.peltoche.fyde.theme.AppDimensions
import com.peltoche.fyde.theme.AppSpacing
import com.peltoche.fyde.theme.AppTypography
import org.jetbrains.compose.resources.stringResource

/**
 * Full-screen PDF preview opened from a [DocumentRow] tap, rendering
 * [document]'s already-decrypted [FydeDocument.content] via the
 * platform-native [PdfViewer].
 */
@Composable
fun PdfViewerScreen(document: FydeDocument, onBack: () -> Unit, modifier: Modifier = Modifier) {
    Column(
        modifier = modifier
            .fillMaxSize()
            .background(AppColors.background),
    ) {
        PdfViewerTopBar(title = document.name, onBack = onBack)
        PdfViewer(
            pdfBytes = document.content,
            modifier = Modifier.weight(1f).fillMaxWidth(),
        )
    }
}

@Composable
private fun PdfViewerTopBar(title: String, onBack: () -> Unit) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(AppDimensions.topBarHeight)
            .background(AppColors.surfaceElevated),
        contentAlignment = Alignment.CenterStart,
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(end = AppSpacing.space5),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            TextButton(onClick = onBack) {
                // "←" stands in for an icon glyph, not language — exempt
                // from the Regular/Medium/SemiBold-only rule real text follows.
                Text(
                    text = "←",
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold,
                    color = AppColors.text,
                )
                Spacer(Modifier.width(AppSpacing.space2))
                Text(
                    text = stringResource(Res.string.back),
                    style = AppTypography.label,
                    color = AppColors.text,
                )
            }
            Spacer(Modifier.width(AppSpacing.space2))
            Text(
                text = title,
                style = AppTypography.section,
                color = AppColors.text,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.weight(1f),
            )
        }
        HorizontalDivider(
            modifier = Modifier.align(Alignment.BottomCenter),
            color = AppColors.border,
        )
    }
}
