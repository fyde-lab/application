package com.peltoche.fyde.components.content

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextOverflow
import com.peltoche.fyde.theme.AppCategoryColor
import com.peltoche.fyde.theme.AppColors
import com.peltoche.fyde.theme.AppDimensions
import com.peltoche.fyde.theme.AppIconSize
import com.peltoche.fyde.theme.AppShapes
import com.peltoche.fyde.theme.AppSpacing
import com.peltoche.fyde.theme.AppTypography

/** One of a [DocumentPass]'s meta cells under its perforation: "Reçu" over "12 oct.". */
data class DocumentPassCell(val label: String, val value: String)

/** Which of design-system.md's pass headlines a [DocumentPass] carries. */
enum class DocumentPassHeadline {
    /** A value (an amount, a date) in `passAmount`. */
    Value,

    /** The document's name — the "Relevé, autre" and "À classer" templates — in `title`, as it can run long. */
    Name,
}

/**
 * The head of a document's detail (V6, D2) — design-system.md § Document
 * pass: [issuer] and [type] on top, a [headline] and the [line] under it, a
 * perforation, then up to three [cells], all on the category's fill. The
 * caller picks the template by document type and passes only the cells whose
 * attribute is set.
 */
@Composable
fun DocumentPass(
    issuer: String?,
    type: String?,
    headline: String,
    line: String?,
    cells: List<DocumentPassCell>,
    color: AppCategoryColor,
    modifier: Modifier = Modifier,
    headlineKind: DocumentPassHeadline = DocumentPassHeadline.Value,
    icon: @Composable (tint: Color) -> Unit,
) {
    Column(
        modifier = modifier
            .fillMaxWidth()
            .clip(AppShapes.card)
            .background(color.fill)
            .padding(top = AppSpacing.space5, bottom = AppSpacing.space5),
    ) {
        Column(modifier = Modifier.padding(horizontal = AppSpacing.space5)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(Modifier.size(AppIconSize.sm), propagateMinConstraints = true) { icon(color.ink) }
                Spacer(Modifier.width(AppSpacing.space3))
                Text(
                    text = issuer.orEmpty(),
                    style = AppTypography.bodyEmphasis,
                    color = color.ink,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.weight(1f),
                )
                if (type != null) {
                    Text(text = type, style = AppTypography.caption, color = color.ink, maxLines = 1)
                }
            }
            Spacer(Modifier.height(AppSpacing.space4))
            Text(
                text = headline,
                style = if (headlineKind == DocumentPassHeadline.Value) AppTypography.passAmount else AppTypography.title,
                color = AppColors.text,
                modifier = Modifier.semantics { heading() },
            )
            if (line != null) {
                Spacer(Modifier.height(AppSpacing.space2))
                Text(text = line, style = AppTypography.bodyEmphasis, color = AppColors.text)
            }
        }
        if (cells.isNotEmpty()) {
            Spacer(Modifier.height(AppSpacing.space5))
            Perforation(color = color.ink.copy(alpha = PERFORATION_ALPHA))
            Spacer(Modifier.height(AppSpacing.space4))
            Row(
                modifier = Modifier.fillMaxWidth().padding(horizontal = AppSpacing.space5),
                horizontalArrangement = Arrangement.spacedBy(AppSpacing.space5),
            ) {
                cells.take(MAX_CELLS).forEach { cell ->
                    Column(modifier = Modifier.weight(1f)) {
                        Text(text = cell.label, style = AppTypography.caption, color = AppColors.categoryCaption, maxLines = 1)
                        Text(
                            text = cell.value,
                            style = AppTypography.bodyEmphasis,
                            color = AppColors.text,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                        )
                    }
                }
                repeat(MAX_CELLS - cells.size.coerceAtMost(MAX_CELLS)) { Spacer(Modifier.weight(1f)) }
            }
        }
    }
}

/** The dashed tear line between a pass's head and its meta cells. */
@Composable
private fun Perforation(color: Color) {
    Canvas(modifier = Modifier.fillMaxWidth().height(AppDimensions.borderFocus)) {
        val dash = AppDimensions.borderFocus.toPx() * 3f
        drawLine(
            color = color,
            start = Offset(0f, size.height / 2f),
            end = Offset(size.width, size.height / 2f),
            strokeWidth = AppDimensions.borderFocus.toPx(),
            pathEffect = PathEffect.dashPathEffect(floatArrayOf(dash, dash)),
        )
    }
}

private const val MAX_CELLS = 3
private const val PERFORATION_ALPHA = 0.4f
