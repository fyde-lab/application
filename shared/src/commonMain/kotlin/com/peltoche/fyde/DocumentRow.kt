package com.peltoche.fyde

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

/** A document received from a fyde server, ready to be shown in a list. */
data class Document(
    val title: String,
    val sizeLabel: String,
    val receivedLabel: String,
)

/** The lime-green accent used to mark PDF files across the document list. */
val PdfAccentColor = Color(0xFFD7EE5B)

/**
 * One row in the "Latest imported documents" list: a file-type icon next to
 * the title and a "size · received" caption, with a hairline divider below.
 */
@Composable
fun DocumentRow(document: Document, modifier: Modifier = Modifier, showDivider: Boolean = true) {
    Column(modifier = modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            PdfFileIcon()
            Spacer(Modifier.width(12.dp))
            Column {
                Text(
                    text = document.title,
                    style = MaterialTheme.typography.bodyLarge,
                    fontWeight = FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.onSurface,
                )
                Spacer(Modifier.height(2.dp))
                Text(
                    text = "${document.sizeLabel} · ${document.receivedLabel}",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
        if (showDivider) {
            HorizontalDivider(
                modifier = Modifier.padding(horizontal = 16.dp),
                color = MaterialTheme.colorScheme.outlineVariant,
            )
        }
    }
}

/** A folded-corner PDF file icon on a rounded chip, drawn without an icon pack. */
@Composable
private fun PdfFileIcon(modifier: Modifier = Modifier) {
    Box(
        modifier = modifier
            .size(44.dp)
            .clip(RoundedCornerShape(10.dp))
            .background(MaterialTheme.colorScheme.surfaceVariant),
    ) {
        Canvas(
            modifier = Modifier
                .align(Alignment.Center)
                .size(20.dp)
                .padding(bottom = 4.dp),
        ) {
            val fold = size.minDimension * 0.32f
            val body = Path().apply {
                moveTo(0f, 0f)
                lineTo(size.width - fold, 0f)
                lineTo(size.width, fold)
                lineTo(size.width, size.height)
                lineTo(0f, size.height)
                close()
            }
            drawPath(body, color = PdfAccentColor, style = Stroke(width = 1.4.dp.toPx()))

            val flap = Path().apply {
                moveTo(size.width - fold, 0f)
                lineTo(size.width - fold, fold)
                lineTo(size.width, fold)
                close()
            }
            drawPath(flap, color = PdfAccentColor, style = Stroke(width = 1.dp.toPx()))
        }
        Text(
            text = "PDF",
            color = PdfAccentColor,
            fontWeight = FontWeight.Bold,
            fontSize = 7.sp,
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .padding(bottom = 6.dp),
        )
    }
}
