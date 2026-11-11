package com.peltoche.fyde

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
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
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import com.peltoche.fyde.components.action.AppTextButton
import com.peltoche.fyde.components.content.AppChip
import com.peltoche.fyde.components.content.AppDivider
import com.peltoche.fyde.components.content.AppGroup
import com.peltoche.fyde.components.content.AppSectionTitle
import com.peltoche.fyde.components.feedback.AppEmptyState
import com.peltoche.fyde.components.form.AppSearchField
import com.peltoche.fyde.components.overlay.AppDropdownMenu
import com.peltoche.fyde.components.overlay.AppDropdownMenuItem
import com.peltoche.fyde.icons.SearchGlyph
import com.peltoche.fyde.resources.Res
import com.peltoche.fyde.resources.cancel
import com.peltoche.fyde.resources.clear_filters
import com.peltoche.fyde.resources.clear_search
import com.peltoche.fyde.resources.documents_count
import com.peltoche.fyde.resources.filter_folder
import com.peltoche.fyde.resources.filter_folder_all
import com.peltoche.fyde.resources.filter_type
import com.peltoche.fyde.resources.filter_type_all
import com.peltoche.fyde.resources.filter_year
import com.peltoche.fyde.resources.filter_year_all
import com.peltoche.fyde.resources.no_documents_match
import com.peltoche.fyde.resources.no_documents_match_filters
import com.peltoche.fyde.resources.no_matches_title
import com.peltoche.fyde.resources.search_results_count
import com.peltoche.fyde.resources.vault_search
import com.peltoche.fyde.resources.vault_search_in
import com.peltoche.fyde.resources.vault_search_local
import com.peltoche.fyde.resources.vault_search_types
import com.peltoche.fyde.theme.AppColors
import com.peltoche.fyde.theme.AppIconSize
import com.peltoche.fyde.theme.AppSpacing
import com.peltoche.fyde.theme.AppTypography
import org.jetbrains.compose.resources.pluralStringResource
import org.jetbrains.compose.resources.stringResource

/**
 * V4 and V5: the field and Annuler, then the type shortcuts until there's
 * something to search for, the facet chips and results after. [selected]
 * marks the document open beside the results (D2's list column).
 */
@Composable
internal fun SearchPage(vault: Vault, page: VaultPage.Search, selected: FydeDocument? = null) {
    val query = page.query
    val contents = vault.state.search?.takeIf { it.query.showsResults == query.showsResults }
    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .then(if (selected != null) Modifier.padding(horizontal = AppSpacing.space5) else Modifier),
    ) {
        SearchBar(vault, page, focusOnOpen = selected == null)
        VaultNotes(vault)
        if (query.showsResults) {
            SearchResults(vault, page, contents, selected)
        } else {
            TypeShortcuts(vault, page, contents)
        }
        Spacer(Modifier.height(AppSpacing.space7))
    }
}

@Composable
private fun SearchBar(vault: Vault, page: VaultPage.Search, focusOnOpen: Boolean) {
    val query = page.query
    val focusRequester = remember { FocusRequester() }
    if (focusOnOpen) LaunchedEffect(Unit) { focusRequester.requestFocus() }
    Row(
        modifier = Modifier.fillMaxWidth().padding(top = AppSpacing.space4),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        AppSearchField(
            value = query.text,
            onValueChange = { vault.search(page.copy(query = query.copy(text = it))) },
            placeholder = query.folder?.let { stringResource(Res.string.vault_search_in, it.label()) }
                ?: stringResource(Res.string.vault_search),
            modifier = Modifier.weight(1f).focusRequester(focusRequester),
            clearContentDescription = stringResource(Res.string.clear_search),
            onClear = { vault.search(page.copy(query = query.copy(text = ""))) },
            leadingIcon = { SearchGlyph(modifier = Modifier.size(AppIconSize.sm)) },
        )
        AppTextButton(text = stringResource(Res.string.cancel), onClick = { vault.openPage(page.origin) })
    }
}

/** V4: the document types the SDK finds in the search's scope, each opening its results. */
@Composable
private fun TypeShortcuts(vault: Vault, page: VaultPage.Search, contents: VaultSearchContents?) {
    val types = contents?.options?.types?.takeIf { it.isNotEmpty() } ?: return
    AppSectionTitle(stringResource(Res.string.vault_search_types))
    FlowRow(
        horizontalArrangement = Arrangement.spacedBy(AppSpacing.space3),
        verticalArrangement = Arrangement.spacedBy(AppSpacing.space3),
    ) {
        types.forEach { facet ->
            AppChip(
                text = facet.value.label(),
                onClick = { vault.search(page.copy(query = page.query.copy(type = facet.value))) },
            )
        }
    }
}

/**
 * V5: the Dossier, Type and Année chips, how many were found, the rows
 * with the typed words marked, and where the search ran.
 */
@Composable
private fun SearchResults(vault: Vault, page: VaultPage.Search, contents: VaultSearchContents?, selected: FydeDocument?) {
    val query = page.query
    fun refine(next: VaultQuery) = vault.search(page.copy(query = next))
    val options = contents?.options ?: VaultFilterOptions()
    FlowRow(
        modifier = Modifier.fillMaxWidth().padding(top = AppSpacing.space5),
        horizontalArrangement = Arrangement.spacedBy(AppSpacing.space3),
        verticalArrangement = Arrangement.spacedBy(AppSpacing.space3),
    ) {
        FacetMenuChip(
            title = stringResource(Res.string.filter_folder),
            allLabel = stringResource(Res.string.filter_folder_all),
            selected = query.folder,
            options = options.folders.map { it.value to it.count },
            optionLabel = { it.label() },
            onSelect = { refine(query.copy(folder = it)) },
        )
        FacetMenuChip(
            title = stringResource(Res.string.filter_type),
            allLabel = stringResource(Res.string.filter_type_all),
            selected = query.type,
            options = options.types.map { it.value to it.count },
            optionLabel = { it.label() },
            onSelect = { refine(query.copy(type = it)) },
        )
        FacetMenuChip(
            title = stringResource(Res.string.filter_year),
            allLabel = stringResource(Res.string.filter_year_all),
            selected = query.year,
            options = options.years.map { it to null },
            optionLabel = { it.label() },
            onSelect = { refine(query.copy(year = it)) },
        )
    }
    when {
        contents == null -> {
            Spacer(Modifier.height(AppSpacing.space7))
            DocumentRowSkeletons()
        }
        contents.documents.isEmpty() -> {
            val searched = contents.query.text.trim()
            val filtered = query.filtersActive
            AppEmptyState(
                modifier = Modifier.padding(top = AppSpacing.space9),
                title = stringResource(Res.string.no_matches_title),
                description = if (searched.isNotEmpty()) {
                    stringResource(Res.string.no_documents_match, searched)
                } else {
                    stringResource(Res.string.no_documents_match_filters)
                },
                actionLabel = stringResource(Res.string.clear_filters).takeIf { filtered },
                onAction = { refine(VaultQuery(text = query.text)) }.takeIf { filtered },
            )
        }
        else -> {
            val searched = contents.query.text.trim()
            val count = contents.documents.size
            Text(
                text = if (searched.isNotEmpty()) {
                    pluralStringResource(Res.plurals.search_results_count, count, count, searched)
                } else {
                    pluralStringResource(Res.plurals.documents_count, count, count)
                },
                style = AppTypography.caption,
                color = AppColors.textMuted,
                modifier = Modifier.padding(top = AppSpacing.space5, bottom = AppSpacing.space3),
            )
            AppGroup {
                contents.documents.forEachIndexed { index, document ->
                    if (index > 0) AppDivider()
                    VaultDocumentRow(
                        document = document,
                        onClick = { vault.openDocument(document) },
                        selected = document.id == selected?.id,
                        highlightTerms = contents.query.terms,
                        showsFolder = true,
                        onDelete = { vault.askDelete(document) }.takeIf { selected != null },
                        onMove = { vault.askMove(document) }.takeIf { selected != null },
                    )
                }
            }
            Text(
                text = stringResource(Res.string.vault_search_local),
                style = AppTypography.caption,
                color = AppColors.textMuted,
                modifier = Modifier.padding(top = AppSpacing.space4),
            )
        }
    }
}

/**
 * A chip opening a menu of one facet's [options]: "all" first, then each
 * choice with how many documents it leads to when known. Its label shows
 * the picked choice. Hidden while there's nothing to pick and nothing picked.
 */
@Composable
private fun <T : Any> FacetMenuChip(
    title: String,
    allLabel: String,
    selected: T?,
    options: List<Pair<T, Long?>>,
    optionLabel: @Composable (T) -> String,
    onSelect: (T?) -> Unit,
) {
    if (options.isEmpty() && selected == null) return
    var expanded by remember { mutableStateOf(false) }
    Box {
        AppChip(
            text = selected?.let { optionLabel(it) } ?: title,
            showChevron = true,
            selected = selected != null,
            onClick = { expanded = true },
        )
        AppDropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }) {
            (listOf<Pair<T?, Long?>>(null to null) + options).forEach { (option, count) ->
                AppDropdownMenuItem(
                    text = option?.let { optionLabel(it) } ?: allLabel,
                    trailingContent = count?.let {
                        { Text(it.toString(), style = AppTypography.caption, color = AppColors.textMuted) }
                    },
                    onClick = {
                        expanded = false
                        onSelect(option)
                    },
                )
            }
        }
    }
}
