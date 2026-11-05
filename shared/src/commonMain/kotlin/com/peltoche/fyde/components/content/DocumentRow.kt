package com.peltoche.fyde.components.content

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Text
import androidx.compose.material3.ripple
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.selected
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import com.peltoche.fyde.theme.AppCategoryColor
import com.peltoche.fyde.theme.AppColors
import com.peltoche.fyde.theme.AppDimensions
import com.peltoche.fyde.theme.AppIconSize
import com.peltoche.fyde.theme.AppRadius
import com.peltoche.fyde.theme.AppShapes
import com.peltoche.fyde.theme.AppSpacing
import com.peltoche.fyde.theme.AppTypography
import com.peltoche.fyde.theme.appFocusRing

/**
 * A document in a list (V3, V5, D2, D6's latest arrivals): its category's
 * tile, [title] over [subtitle] ("EDF · 12 oct."), and its [amount]
 * ("84,20 €", in `data`) at the end when it has one. [highlightTerms] are marked in the title wherever they
 * occur, ignoring case — the words a search matched. [selected] is the
 * document open beside the list (D2), drawn in the category's fill.
 */
@Composable
fun DocumentRow(
    title: String,
    subtitle: String?,
    amount: String?,
    color: AppCategoryColor,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    selected: Boolean = false,
    highlightTerms: List<String> = emptyList(),
    icon: @Composable (tint: Color) -> Unit,
) {
    val interactionSource = remember { MutableInteractionSource() }
    Row(
        modifier = modifier
            .fillMaxWidth()
            .background(if (selected) color.fill else Color.Transparent)
            .clickable(interactionSource = interactionSource, indication = ripple(), role = Role.Button, onClick = onClick)
            .appFocusRing(interactionSource, shape = AppRadius.none)
            .semantics(mergeDescendants = true) { this.selected = selected }
            .heightIn(min = AppDimensions.rowHeightStandard)
            .padding(horizontal = AppSpacing.space5, vertical = AppSpacing.space3),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(AppSpacing.space4),
    ) {
        CategoryFillTile(size = AppDimensions.iconTile, color = color, icon = icon)
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = highlighted(title, highlightTerms, AppColors.highlight),
                style = AppTypography.bodyEmphasis,
                color = AppColors.text,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
            if (subtitle != null) {
                Text(text = subtitle, style = AppTypography.caption, color = AppColors.textMuted, maxLines = 1, overflow = TextOverflow.Ellipsis)
            }
        }
        if (amount != null) {
            Text(text = amount, style = AppTypography.data, color = AppColors.text, maxLines = 1)
        }
    }
}

/**
 * A document in a category's grid at Expanded (D1): a preview well holding
 * its category's tile, then [title] and [caption] (its date).
 */
@Composable
fun DocumentCard(
    title: String,
    caption: String?,
    color: AppCategoryColor,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    icon: @Composable (tint: Color) -> Unit,
) {
    val interactionSource = remember { MutableInteractionSource() }
    Column(
        modifier = modifier
            .clip(AppShapes.card)
            .background(AppColors.surface)
            .border(AppDimensions.borderHairline, AppColors.border, AppShapes.card)
            .clickable(interactionSource = interactionSource, indication = ripple(), role = Role.Button, onClick = onClick)
            .appFocusRing(interactionSource, shape = AppShapes.card)
            .semantics(mergeDescendants = true) {},
    ) {
        Box(
            modifier = Modifier.fillMaxWidth().height(AppDimensions.documentCardWell).background(AppColors.background),
            contentAlignment = Alignment.Center,
        ) {
            CategoryFillTile(size = AppDimensions.iconTileLarge, color = color, icon = icon)
        }
        Column(modifier = Modifier.padding(AppSpacing.space4)) {
            Text(text = title, style = AppTypography.bodyEmphasis, color = AppColors.text, maxLines = 1, overflow = TextOverflow.Ellipsis)
            if (caption != null) {
                Text(text = caption, style = AppTypography.caption, color = AppColors.textMuted, maxLines = 1)
            }
        }
    }
}

/** A tile in the category's own fill, its icon in the category's ink — for a category's mark off its fill. */
@Composable
fun CategoryFillTile(
    size: Dp,
    color: AppCategoryColor,
    modifier: Modifier = Modifier,
    shape: Shape = AppShapes.iconTile,
    iconSize: Dp = AppIconSize.md,
    icon: @Composable (tint: Color) -> Unit,
) {
    Box(
        modifier = modifier.size(size).clip(shape).background(color.fill),
        contentAlignment = Alignment.Center,
    ) {
        Box(Modifier.size(iconSize), propagateMinConstraints = true) { icon(color.ink) }
    }
}

/**
 * [text] with every occurrence of each of [terms] on [background], ignoring
 * case and accents the way the SDK's search matches them.
 */
internal fun highlighted(text: String, terms: List<String>, background: Color): AnnotatedString {
    val needles = terms.map { folded(it.trim()) }.filter { it.isNotEmpty() }
    if (needles.isEmpty()) return AnnotatedString(text)
    val haystack = folded(text)
    val marked = BooleanArray(text.length)
    for (needle in needles) {
        var from = haystack.indexOf(needle)
        while (from >= 0) {
            for (i in from until from + needle.length) marked[i] = true
            from = haystack.indexOf(needle, startIndex = from + needle.length)
        }
    }
    return buildAnnotatedString {
        append(text)
        var start = -1
        for (i in 0..text.length) {
            val on = i < text.length && marked[i]
            if (on && start < 0) start = i
            if (!on && start >= 0) {
                addStyle(SpanStyle(background = background), start, i)
                start = -1
            }
        }
    }
}

/** [text] lowercased with its accents dropped, one character for one so indices still line up. */
private fun folded(text: String): String = buildString(text.length) {
    for (c in text) {
        val lower = c.lowercaseChar()
        val i = ACCENTED.indexOf(lower)
        append(if (i >= 0) UNACCENTED[i] else lower)
    }
}

private const val ACCENTED = "àáâãäåçèéêëìíîïñòóôõöùúûüýÿ"
private const val UNACCENTED = "aaaaaaceeeeiiiinooooouuuuyy"
