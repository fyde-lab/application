package com.peltoche.fyde

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextOverflow
import com.peltoche.fyde.components.action.AppBackButton
import com.peltoche.fyde.components.action.AppCircleButton
import com.peltoche.fyde.components.content.AppChip
import com.peltoche.fyde.components.content.AppDivider
import com.peltoche.fyde.components.content.CategoryBand
import com.peltoche.fyde.components.content.DocumentCard
import com.peltoche.fyde.components.feedback.AppEmptyState
import com.peltoche.fyde.resources.Res
import com.peltoche.fyde.resources.clear_filters
import com.peltoche.fyde.resources.documents_count
import com.peltoche.fyde.resources.no_documents_match_filters
import com.peltoche.fyde.resources.no_matches_title
import com.peltoche.fyde.resources.vault_all_count
import com.peltoche.fyde.resources.vault_category_overline
import com.peltoche.fyde.resources.vault_facet_count
import com.peltoche.fyde.resources.vault_sort_newest
import com.peltoche.fyde.resources.vault_sort_oldest
import com.peltoche.fyde.resources.vault_year_before
import com.peltoche.fyde.theme.AppColors
import com.peltoche.fyde.theme.AppIconSize
import com.peltoche.fyde.theme.AppSpacing
import com.peltoche.fyde.theme.AppTypography
import org.jetbrains.compose.resources.pluralStringResource
import org.jetbrains.compose.resources.stringResource

/** V3: a category's band, its chips, and its documents by month. */
@Composable
internal fun CategoryPage(vault: Vault, page: VaultPage.Category) {
    val contents = vault.contents(page)
    val count = vault.categoryCount(page.category)
    Column(modifier = Modifier.fillMaxSize().verticalScroll(rememberScrollState())) {
        VaultPageBar(title = null, onBack = { vault.openPage(VaultPage.Home) }, trailing = { SortButton(vault, page) })
        CategoryBand(
            name = page.category.label(),
            subtitle = count?.let { pluralStringResource(Res.plurals.documents_count, it.toInt(), it.toInt()) },
            color = page.category.colors(),
            icon = { tint -> CategoryGlyph(page.category, tint) },
        )
        VaultNotes(vault)
        CategoryChips(vault, page, contents)
        CategoryDocuments(vault, page, contents) { documents -> CategoryRows(vault, documents, selected = null) }
        Spacer(Modifier.height(AppSpacing.space7))
    }
}

/** D1: a category's documents as a grid of cards, by month. */
@Composable
internal fun CategoryGridPage(vault: Vault, page: VaultPage.Category) {
    val contents = vault.contents(page)
    Column(modifier = Modifier.fillMaxSize().verticalScroll(rememberScrollState())) {
        Spacer(Modifier.height(AppSpacing.space5))
        Row(verticalAlignment = Alignment.CenterVertically) {
            AppBackButton(onClick = { vault.openPage(VaultPage.Home) })
            Spacer(Modifier.width(AppSpacing.space3))
            Column(modifier = Modifier.weight(1f)) {
                Text(text = stringResource(Res.string.vault_category_overline), style = AppTypography.caption, color = AppColors.textMuted)
                Text(
                    text = page.category.label(),
                    style = AppTypography.display,
                    color = AppColors.text,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.semantics { heading() },
                )
            }
            SortButton(vault, page)
            Spacer(Modifier.width(AppSpacing.space3))
            ImportAction(vault)
        }
        VaultNotes(vault)
        CategoryChips(vault, page, contents)
        CategoryDocuments(vault, page, contents) { documents ->
            VaultGrid(items = documents, columns = 4) { document ->
                DocumentCard(
                    title = document.name,
                    caption = localizedShortDate(document.createdAt),
                    color = document.category.colors(),
                    onClick = { vault.openDocument(document) },
                    icon = { tint -> CategoryGlyph(document.category, tint) },
                )
            }
        }
        Spacer(Modifier.height(AppSpacing.space7))
    }
}

/** D2's list column: the open category's chips and rows, [selected] marked, beside its pass. */
@Composable
internal fun CategoryListColumn(vault: Vault, page: VaultPage.Category, selected: FydeDocument) {
    val contents = vault.contents(page)
    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = AppSpacing.space5),
    ) {
        VaultPageBar(title = page.category.label(), onBack = vault::closeDocument, trailing = { SortButton(vault, page) })
        VaultNotes(vault)
        CategoryChips(vault, page, contents)
        CategoryDocuments(vault, page, contents) { documents -> CategoryRows(vault, documents, selected = selected) }
        Spacer(Modifier.height(AppSpacing.space7))
    }
}

/** Flips the page between newest and oldest first; read out as the order it's on. */
@Composable
private fun SortButton(vault: Vault, page: VaultPage.Category) {
    AppCircleButton(
        onClick = { vault.refine(page.copy(newestFirst = !page.newestFirst)) },
        contentDescription = stringResource(if (page.newestFirst) Res.string.vault_sort_newest else Res.string.vault_sort_oldest),
    ) {
        SortGlyph(modifier = Modifier.size(AppIconSize.md), tint = AppColors.text)
    }
}

/**
 * "Tout" and the subcategories the SDK finds documents in, then the years:
 * each a single choice, picked again to drop it.
 */
@Composable
private fun CategoryChips(vault: Vault, page: VaultPage.Category, contents: VaultCategoryContents?) {
    val options = contents?.options ?: return
    if (options.subcategories.isNotEmpty()) {
        FlowRow(
            modifier = Modifier.fillMaxWidth().padding(top = AppSpacing.space5),
            horizontalArrangement = Arrangement.spacedBy(AppSpacing.space3),
            verticalArrangement = Arrangement.spacedBy(AppSpacing.space3),
        ) {
            AppChip(
                text = stringResource(Res.string.vault_all_count, contents.allCount),
                selected = page.subcategory == null,
                onClick = { vault.refine(page.copy(subcategory = null)) },
            )
            options.subcategories.forEach { facet ->
                val selected = page.subcategory == facet.value
                AppChip(
                    text = stringResource(Res.string.vault_facet_count, facet.value.label(), facet.count),
                    selected = selected,
                    onClick = { vault.refine(page.copy(subcategory = if (selected) null else facet.value)) },
                )
            }
        }
    }
    if (options.years.isNotEmpty()) {
        FlowRow(
            modifier = Modifier.fillMaxWidth().padding(top = AppSpacing.space3),
            horizontalArrangement = Arrangement.spacedBy(AppSpacing.space3),
            verticalArrangement = Arrangement.spacedBy(AppSpacing.space3),
        ) {
            options.years.forEach { year ->
                val selected = page.year == year
                AppChip(
                    text = year.label(),
                    selected = selected,
                    onClick = { vault.refine(page.copy(year = if (selected) null else year)) },
                )
            }
        }
    }
}

@Composable
private fun YearFilter.label(): String = when (this) {
    is YearFilter.In -> year.toString()
    is YearFilter.Before -> stringResource(Res.string.vault_year_before, year)
}

/** The page's documents under their month headings, [documents] drawing each month's; skeletons while loading. */
@Composable
private fun CategoryDocuments(
    vault: Vault,
    page: VaultPage.Category,
    contents: VaultCategoryContents?,
    documents: @Composable (List<FydeDocument>) -> Unit,
) {
    when {
        contents == null -> {
            Spacer(Modifier.height(AppSpacing.space7))
            DocumentRowSkeletons()
        }
        contents.documents.isEmpty() -> {
            val filtered = page.subcategory != null || page.year != null
            AppEmptyState(
                modifier = Modifier.padding(top = AppSpacing.space9),
                title = stringResource(Res.string.no_matches_title),
                description = stringResource(Res.string.no_documents_match_filters),
                actionLabel = stringResource(Res.string.clear_filters).takeIf { filtered },
                onAction = { vault.refine(page.copy(subcategory = null, year = null)) }.takeIf { filtered },
            )
        }
        else -> {
            val groups = remember(contents.documents) { monthGroups(contents.documents) }
            groups.forEach { group ->
                MonthHeading(group)
                documents(group.documents)
            }
        }
    }
}

@Composable
private fun MonthHeading(group: MonthGroup) {
    Row(
        modifier = Modifier.fillMaxWidth().padding(top = AppSpacing.space7, bottom = AppSpacing.space3),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(
            text = localizedMonthHeading(group.year, group.month),
            style = AppTypography.section,
            color = AppColors.text,
            modifier = Modifier.weight(1f).semantics { heading() },
        )
        val count = group.documents.size
        Text(text = pluralStringResource(Res.plurals.documents_count, count, count), style = AppTypography.caption, color = AppColors.textMuted)
    }
}

/** One month's rows: "Énergie · Facture" under each name, [selected] marked. */
@Composable
private fun CategoryRows(vault: Vault, documents: List<FydeDocument>, selected: FydeDocument?) {
    SettingsGroup {
        documents.forEachIndexed { index, document ->
            if (index > 0) AppDivider()
            VaultDocumentRow(
                document = document,
                subtitle = listOfNotNull(document.subcategory?.label(), document.documentType?.label()).joinToString(" · "),
                onClick = { vault.openDocument(document) },
                selected = document.id == selected?.id,
            )
        }
    }
}
