package com.peltoche.fyde

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier

/**
 * Renders [pdfBytes] (a whole PDF file, already decrypted by `fyde-sdk`) as a
 * vertically scrollable stack of pages, using each platform's own PDF engine
 * (Android's `PdfRenderer`, iOS's PDFKit, JVM's PDFBox) so no PDF-parsing
 * library needs to be shared across targets. Not yet implemented for Web,
 * see `../../../CLAUDE.md`.
 */
@Composable
expect fun PdfViewer(pdfBytes: ByteArray, modifier: Modifier = Modifier)
