package com.peltoche.fyde

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import com.peltoche.fyde.components.navigation.AppTopBar
import com.peltoche.fyde.resources.Res
import com.peltoche.fyde.resources.back
import com.peltoche.fyde.theme.AppColors
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
        AppTopBar(
            title = document.name,
            onBack = onBack,
            backContentDescription = stringResource(Res.string.back),
        )
        PdfViewer(
            pdfBytes = document.content,
            modifier = Modifier.weight(1f).fillMaxWidth(),
        )
    }
}
