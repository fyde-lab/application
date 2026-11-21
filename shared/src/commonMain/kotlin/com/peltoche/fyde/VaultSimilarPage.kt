package com.peltoche.fyde

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import com.peltoche.fyde.components.action.AppButton
import com.peltoche.fyde.components.action.AppButtonVariant
import com.peltoche.fyde.components.action.SelectionBar
import com.peltoche.fyde.components.content.AppDivider
import com.peltoche.fyde.components.content.AppGroup
import com.peltoche.fyde.components.content.DocumentRow
import com.peltoche.fyde.components.content.SimilarityBadge
import com.peltoche.fyde.components.feedback.AppEmptyState
import com.peltoche.fyde.components.navigation.AppPageBar
import com.peltoche.fyde.icons.FolderGlyph
import com.peltoche.fyde.icons.TrashGlyph
import com.peltoche.fyde.layout.AppWindowSizeClass
import com.peltoche.fyde.layout.LocalAppWindowSizeClass
import com.peltoche.fyde.resources.Res
import com.peltoche.fyde.resources.deselect_all
import com.peltoche.fyde.resources.document_move
import com.peltoche.fyde.resources.documents_count
import com.peltoche.fyde.resources.select_all
import com.peltoche.fyde.resources.select_document
import com.peltoche.fyde.resources.selected_count
import com.peltoche.fyde.resources.similarity_description
import com.peltoche.fyde.resources.similarity_percent
import com.peltoche.fyde.resources.vault_delete_confirm
import com.peltoche.fyde.resources.vault_similar
import com.peltoche.fyde.resources.vault_similar_local
import com.peltoche.fyde.resources.vault_similar_none
import com.peltoche.fyde.resources.vault_similar_none_title
import com.peltoche.fyde.resources.vault_similar_see_all
import com.peltoche.fyde.resources.vault_similar_this
import com.peltoche.fyde.theme.AppColors
import com.peltoche.fyde.theme.AppDimensions
import com.peltoche.fyde.theme.AppIconSize
import com.peltoche.fyde.theme.AppSpacing
import com.peltoche.fyde.theme.AppTypography
import kotlin.math.floor
import org.jetbrains.compose.resources.pluralStringResource
import org.jetbrains.compose.resources.stringResource

/** How many similar documents a document's section shows (V6c, D2). */
internal const val SIMILAR_SECTION_LIMIT = 3L

/** How many the similar page (V16, D11) asks the SDK for. */
internal const val SIMILAR_PAGE_LIMIT = 50L

/**
 * The SDK's confidence as a whole percentage, rounded down so that only
 * identical text reads 100 (design-system.md § Similar documents). The
 * epsilon keeps a float like 0.94f, stored as 0.93999…, at 94.
 */
internal fun similarityPercent(confidence: Float): Int =
    floor(confidence.toDouble() * 100 + 1e-4).toInt().coerceIn(0, 100)

/**
 * V6c / D2: the three documents closest to [document] under "Tout voir",
 * which opens V16 (D11 keeping [document] open beside it when [keepOpen]).
 * Hidden while loading, when there's none, and beside D11's list of them.
 */
@Composable
internal fun SimilarSection(vault: Vault, document: FydeDocument, keepOpen: Boolean) {
    val similar = vault.state.documentSimilar?.takeIf { it.documentId == document.id }?.documents
    if (similar.isNullOrEmpty()) return
    if ((vault.focus.page as? VaultPage.Similar)?.document?.id == document.id && keepOpen) return
    SectionTitleWithAction(
        title = stringResource(Res.string.vault_similar),
        action = stringResource(Res.string.vault_similar_see_all),
        onAction = { vault.openSimilar(document, keepOpen) },
        adds = false,
    )
    AppGroup {
        similar.take(SIMILAR_SECTION_LIMIT.toInt()).forEachIndexed { index, item ->
            if (index > 0) AppDivider()
            SimilarDocumentRow(document = item.document, confidence = item.confidence, onClick = { vault.openDocument(item.document) })
        }
    }
}

/**
 * V16 (a page below Expanded) and D11's list column ([selected] being the
 * document open beside it): the document it started from, then every
 * similar document with a checkbox, most similar first, and the selection
 * bar while some are ticked (design-system.md § Selection).
 */
@Composable
internal fun SimilarPage(vault: Vault, page: VaultPage.Similar, selected: FydeDocument? = null) {
    val wide = LocalAppWindowSizeClass.current == AppWindowSizeClass.Expanded
    val contents = vault.state.similar?.takeIf { it.documentId == page.document.id }
    val rows = listOf(page.document) + contents?.documents.orEmpty().map { it.document }
    // Ticks of documents gone meanwhile (deleted, here or elsewhere) don't count.
    val ticked = page.ticked.filterTo(mutableSetOf()) { id -> rows.any { it.id == id } }
    fun toggle(document: FydeDocument) =
        vault.tick(page, if (document.id in ticked) ticked - document.id else ticked + document.id)
    // Below Expanded a tap ticks once something is; at Expanded it always opens (D11).
    fun onRowClick(document: FydeDocument) = if (!wide && ticked.isNotEmpty()) toggle(document) else vault.openDocument(document)

    Column(modifier = Modifier.fillMaxSize()) {
        Column(
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth()
                .verticalScroll(rememberScrollState())
                .then(if (selected != null) Modifier.padding(horizontal = AppSpacing.space5) else Modifier),
        ) {
            AppPageBar(title = stringResource(Res.string.vault_similar), onBack = { vault.closeSimilar(page) })
            VaultNotes(vault)
            Text(
                text = stringResource(Res.string.vault_similar_local),
                style = AppTypography.caption,
                color = AppColors.textMuted,
                modifier = Modifier.padding(top = AppSpacing.space3, bottom = AppSpacing.space4),
            )
            AppGroup {
                SimilarDocumentRow(
                    document = page.document,
                    confidence = null,
                    onClick = { onRowClick(page.document) },
                    selected = page.document.id == selected?.id,
                    checked = page.document.id in ticked,
                    onCheckedChange = { toggle(page.document) },
                )
            }
            when {
                contents == null -> {
                    Spacer(Modifier.height(AppSpacing.space7))
                    DocumentRowSkeletons()
                }
                contents.documents.isEmpty() -> AppEmptyState(
                    modifier = Modifier.padding(top = AppSpacing.space9),
                    title = stringResource(Res.string.vault_similar_none_title),
                    description = stringResource(Res.string.vault_similar_none),
                )
                else -> {
                    val count = contents.documents.size
                    val all = rows.all { it.id in ticked }
                    SectionTitleWithAction(
                        title = pluralStringResource(Res.plurals.documents_count, count, count),
                        action = stringResource(if (all) Res.string.deselect_all else Res.string.select_all),
                        onAction = { vault.tick(page, if (all) emptySet() else rows.mapTo(mutableSetOf()) { it.id }) },
                        adds = false,
                    )
                    AppGroup {
                        contents.documents.forEachIndexed { index, item ->
                            if (index > 0) AppDivider()
                            SimilarDocumentRow(
                                document = item.document,
                                confidence = item.confidence,
                                onClick = { onRowClick(item.document) },
                                selected = item.document.id == selected?.id,
                                checked = item.document.id in ticked,
                                onCheckedChange = { toggle(item.document) },
                            )
                        }
                    }
                }
            }
            Spacer(Modifier.height(AppSpacing.space7))
        }
        if (ticked.isNotEmpty()) {
            val documents = rows.filter { it.id in ticked }
            SelectionBar(
                count = pluralStringResource(Res.plurals.selected_count, documents.size, documents.size),
                clearDescription = stringResource(Res.string.deselect_all),
                onClear = { vault.tick(page, emptySet()) },
                stacked = LocalAppWindowSizeClass.current == AppWindowSizeClass.Compact || selected != null,
            ) {
                AppButton(
                    text = stringResource(Res.string.document_move),
                    onClick = { vault.askMove(documents) },
                    modifier = Modifier.weight(1f),
                    leadingIcon = { FolderGlyph(modifier = Modifier.size(AppIconSize.sm), tint = AppColors.onPrimary) },
                )
                AppButton(
                    text = stringResource(Res.string.vault_delete_confirm),
                    onClick = { vault.askDelete(documents) },
                    modifier = Modifier.weight(1f),
                    variant = AppButtonVariant.Outlined,
                    destructive = true,
                    leadingIcon = { TrashGlyph(modifier = Modifier.size(AppIconSize.sm), tint = AppColors.error) },
                )
            }
        }
    }
}

/**
 * A similar document's row: its path · date and its ressemblance, or "Ce
 * document" for the one the page started from ([confidence] `null`). With
 * [checked], a checkbox leads it.
 */
@Composable
private fun SimilarDocumentRow(
    document: FydeDocument,
    confidence: Float?,
    onClick: () -> Unit,
    selected: Boolean = false,
    checked: Boolean? = null,
    onCheckedChange: (Boolean) -> Unit = {},
) {
    DocumentRow(
        title = document.name,
        subtitle = listOf(document.folderLabel(), localizedShortDate(document.createdAt)).joinToString(" · "),
        amount = null,
        color = document.colors(),
        onClick = onClick,
        selected = selected,
        checked = checked,
        onCheckedChange = onCheckedChange,
        checkDescription = stringResource(Res.string.select_document, document.name),
        thumbnail = rememberDocumentThumbnail(document),
        trailing = {
            if (confidence != null) {
                val percent = similarityPercent(confidence)
                SimilarityBadge(
                    text = stringResource(Res.string.similarity_percent, percent),
                    description = stringResource(Res.string.similarity_description, percent),
                )
            } else {
                Text(text = stringResource(Res.string.vault_similar_this), style = AppTypography.caption, color = AppColors.textMuted)
            }
        },
        icon = { tint -> DocumentFolderGlyph(document, tint) },
    )
}
