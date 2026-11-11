package com.peltoche.fyde

import androidx.compose.foundation.clickable
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
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextOverflow
import com.peltoche.fyde.components.action.AppBackButton
import com.peltoche.fyde.components.action.AppCircleButton
import com.peltoche.fyde.components.content.AppChip
import com.peltoche.fyde.components.content.AppDivider
import com.peltoche.fyde.components.content.AppGroup
import com.peltoche.fyde.components.content.AppSectionTitle
import com.peltoche.fyde.components.content.DocumentCard
import com.peltoche.fyde.components.content.DocumentRow
import com.peltoche.fyde.components.content.FolderBand
import com.peltoche.fyde.components.feedback.AppEmptyState
import com.peltoche.fyde.components.navigation.AppPageBar
import com.peltoche.fyde.icons.FolderGlyph
import com.peltoche.fyde.icons.MoreGlyph
import com.peltoche.fyde.icons.SortGlyph
import com.peltoche.fyde.resources.Res
import com.peltoche.fyde.resources.clear_filters
import com.peltoche.fyde.resources.documents_count
import com.peltoche.fyde.resources.folder_actions
import com.peltoche.fyde.resources.folder_new_short
import com.peltoche.fyde.resources.no_documents_match_filters
import com.peltoche.fyde.resources.no_matches_title
import com.peltoche.fyde.resources.subfolders_count
import com.peltoche.fyde.resources.vault_folder_documents
import com.peltoche.fyde.resources.vault_folder_empty_description
import com.peltoche.fyde.resources.vault_folder_empty_title
import com.peltoche.fyde.resources.vault_folder_overline
import com.peltoche.fyde.resources.vault_import
import com.peltoche.fyde.resources.vault_sort_newest
import com.peltoche.fyde.resources.vault_sort_oldest
import com.peltoche.fyde.resources.vault_subfolders
import com.peltoche.fyde.resources.vault_unclassified
import com.peltoche.fyde.resources.vault_unfiled_description
import com.peltoche.fyde.resources.vault_year_before
import com.peltoche.fyde.theme.AppColors
import com.peltoche.fyde.theme.AppIconSize
import com.peltoche.fyde.theme.AppSpacing
import com.peltoche.fyde.theme.AppTypography
import org.jetbrains.compose.resources.pluralStringResource
import org.jetbrains.compose.resources.stringResource

/**
 * V3: back to the parent, the path, the folder's band (its description
 * under it), its subfolders, then its documents by month. Documents count
 * and list everything below the folder: the SDK's folder filter includes
 * subfolders.
 */
@Composable
internal fun FolderPage(vault: Vault, page: VaultPage.Folder) {
    val contents = vault.contents(page)
    val folders = LocalVaultFolders.current
    val folder = folders[page.folderId]
    Column(modifier = Modifier.fillMaxSize().verticalScroll(rememberScrollState())) {
        AppPageBar(title = null, onBack = { vault.openParent(page) }, trailing = { FolderActions(vault, page) })
        FolderBreadcrumb(vault, page)
        FolderBand(
            name = folderName(page),
            subtitle = contents?.let { folderSubtitle(it) },
            color = folders.appearanceOf(page.folderId).colors(),
            description = folder?.description ?: stringResource(Res.string.vault_unfiled_description).takeIf { page.folderId == null },
            icon = { tint -> if (folder != null) FolderMark(folder, tint) else FolderIconGlyph(null, tint) },
        )
        VaultNotes(vault)
        val subfolders = contents?.subfolders.orEmpty()
        if (subfolders.isNotEmpty()) {
            SubfoldersTitle(vault, page)
            SubfolderRows(vault, subfolders)
        }
        FolderDocuments(vault, page, contents) { documents -> FolderRows(vault, documents, selected = null) }
        Spacer(Modifier.height(AppSpacing.space7))
    }
}

/** D1: the folder's header and description, its subfolders as cards, then its documents as cards by month. */
@Composable
internal fun FolderGridPage(vault: Vault, page: VaultPage.Folder) {
    val contents = vault.contents(page)
    val folder = LocalVaultFolders.current[page.folderId]
    Column(modifier = Modifier.fillMaxSize().verticalScroll(rememberScrollState())) {
        Spacer(Modifier.height(AppSpacing.space5))
        Row(verticalAlignment = Alignment.CenterVertically) {
            AppBackButton(onClick = { vault.openParent(page) })
            Spacer(Modifier.width(AppSpacing.space3))
            Column(modifier = Modifier.weight(1f)) {
                Text(text = stringResource(Res.string.vault_folder_overline), style = AppTypography.caption, color = AppColors.textMuted)
                Text(
                    text = folderName(page),
                    style = AppTypography.display,
                    color = AppColors.text,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.semantics { heading() },
                )
            }
            SortButton(vault, page)
            Spacer(Modifier.width(AppSpacing.space3))
            SearchLauncher(vault, page.filter)
            Spacer(Modifier.width(AppSpacing.space3))
            ImportAction(vault)
            FolderMoreButton(vault, page)
        }
        FolderBreadcrumb(vault, page)
        val description = folder?.description ?: stringResource(Res.string.vault_unfiled_description).takeIf { page.folderId == null }
        if (!description.isNullOrBlank()) {
            Text(
                text = description,
                style = AppTypography.body,
                color = AppColors.textMuted,
                modifier = Modifier.padding(top = AppSpacing.space3),
            )
        }
        VaultNotes(vault)
        val subfolders = contents?.subfolders.orEmpty()
        if (subfolders.isNotEmpty()) {
            SubfoldersTitle(vault, page)
            VaultGrid(items = subfolders, columns = 4) { summary -> FolderSummaryCard(vault, summary) }
        }
        FolderDocuments(vault, page, contents) { documents ->
            VaultGrid(items = documents, columns = 4) { document ->
                DocumentCard(
                    title = document.name,
                    caption = localizedShortDate(document.createdAt),
                    color = document.colors(),
                    onClick = { vault.openDocument(document) },
                    icon = { tint -> DocumentFolderGlyph(document, tint) },
                )
            }
        }
        Spacer(Modifier.height(AppSpacing.space7))
    }
}

/** D2's list column: the open folder's subfolders and rows, [selected] marked, beside its pass. */
@Composable
internal fun FolderListColumn(vault: Vault, page: VaultPage.Folder, selected: FydeDocument) {
    val contents = vault.contents(page)
    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = AppSpacing.space5),
    ) {
        AppPageBar(title = folderName(page), onBack = vault::closeDocument, trailing = { FolderActions(vault, page) })
        FolderBreadcrumb(vault, page)
        VaultNotes(vault)
        val subfolders = contents?.subfolders.orEmpty()
        if (subfolders.isNotEmpty()) {
            SubfoldersTitle(vault, page)
            SubfolderRows(vault, subfolders)
        }
        FolderDocuments(vault, page, contents) { documents -> FolderRows(vault, documents, selected = selected) }
        Spacer(Modifier.height(AppSpacing.space7))
    }
}

/** The open folder's name, "À classer" for the root. */
@Composable
private fun folderName(page: VaultPage.Folder): String =
    LocalVaultFolders.current[page.folderId]?.name ?: stringResource(Res.string.vault_unclassified)

/** "2 sous-dossiers · 38 documents", the subfolders left out when there are none. */
@Composable
private fun folderSubtitle(contents: VaultFolderContents): String {
    val documents = pluralStringResource(Res.plurals.documents_count, contents.count.toInt(), contents.count.toInt())
    val subfolders = contents.subfolders.size
    return if (subfolders > 0) {
        "${pluralStringResource(Res.plurals.subfolders_count, subfolders, subfolders)} · $documents"
    } else {
        documents
    }
}

/**
 * The folders above the open one, top-level first, each opening its page:
 * left out at the top level and for À classer.
 */
@Composable
private fun FolderBreadcrumb(vault: Vault, page: VaultPage.Folder) {
    val ancestors = LocalVaultFolders.current.path(page.folderId).dropLast(1)
    if (ancestors.isEmpty()) return
    FlowRow(
        modifier = Modifier.fillMaxWidth().padding(top = AppSpacing.space3, bottom = AppSpacing.space3),
        verticalArrangement = Arrangement.Center,
    ) {
        ancestors.forEach { folder ->
            Text(
                text = folder.name,
                style = AppTypography.caption,
                color = AppColors.primary,
                modifier = Modifier.clickable(role = Role.Button) { vault.openPage(VaultPage.Folder(folder.id)) },
            )
            Text(text = FOLDER_PATH_SEPARATOR, style = AppTypography.caption, color = AppColors.textMuted)
        }
    }
}

/** A page bar's search within the folder, its sort and "…" (V13). */
@Composable
private fun FolderActions(vault: Vault, page: VaultPage.Folder) {
    Row(horizontalArrangement = Arrangement.spacedBy(AppSpacing.space3)) {
        SearchLauncher(vault, page.filter, asField = false)
        SortButton(vault, page)
        FolderMoreButton(vault, page, leadingSpace = false)
    }
}

/** "…" opening the folder's actions (V13, D7); À classer has none. */
@Composable
private fun FolderMoreButton(vault: Vault, page: VaultPage.Folder, leadingSpace: Boolean = true) {
    val folderId = page.folderId ?: return
    if (leadingSpace) Spacer(Modifier.width(AppSpacing.space3))
    AppCircleButton(
        onClick = { vault.openFolderSheet(FolderSheet.Actions(folderId)) },
        contentDescription = stringResource(Res.string.folder_actions),
    ) {
        MoreGlyph(modifier = Modifier.size(AppIconSize.md), tint = AppColors.text)
    }
}

/** SOUS-DOSSIERS with "+ Nouveau" (V12 in this folder). */
@Composable
private fun SubfoldersTitle(vault: Vault, page: VaultPage.Folder) {
    val folderId = page.folderId ?: return AppSectionTitle(stringResource(Res.string.vault_subfolders))
    SectionTitleWithAction(
        title = stringResource(Res.string.vault_subfolders),
        action = stringResource(Res.string.folder_new_short),
        onAction = { vault.openFolderSheet(FolderSheet.Create(folderId)) },
    )
}

/** Flips the page between newest and oldest first; read out as the order it's on. */
@Composable
private fun SortButton(vault: Vault, page: VaultPage.Folder) {
    AppCircleButton(
        onClick = { vault.refine(page.copy(newestFirst = !page.newestFirst)) },
        contentDescription = stringResource(if (page.newestFirst) Res.string.vault_sort_newest else Res.string.vault_sort_oldest),
    ) {
        SortGlyph(modifier = Modifier.size(AppIconSize.md), tint = AppColors.text)
    }
}

/** One row per subfolder: its tile, name and count, opening its page. */
@Composable
private fun SubfolderRows(vault: Vault, subfolders: List<VaultFolderSummary>) {
    val folders = LocalVaultFolders.current
    AppGroup {
        subfolders.forEachIndexed { index, summary ->
            val folder = summary.folder ?: return@forEachIndexed
            if (index > 0) AppDivider()
            DocumentRow(
                title = folder.name,
                subtitle = pluralStringResource(Res.plurals.documents_count, summary.count.toInt(), summary.count.toInt()),
                amount = null,
                color = folders.appearanceOf(folder.id).colors(),
                onClick = { vault.openPage(VaultPage.Folder(folder.id)) },
                icon = { tint -> FolderGlyph(tint = tint) },
            )
        }
    }
}

/** The year chips, each a single choice, picked again to drop it. */
@Composable
private fun YearChips(vault: Vault, page: VaultPage.Folder, contents: VaultFolderContents?) {
    val years = contents?.options?.years.orEmpty()
    if (years.isEmpty()) return
    FlowRow(
        modifier = Modifier.fillMaxWidth().padding(top = AppSpacing.space3),
        horizontalArrangement = Arrangement.spacedBy(AppSpacing.space3),
        verticalArrangement = Arrangement.spacedBy(AppSpacing.space3),
    ) {
        years.forEach { year ->
            val selected = page.year == year
            AppChip(
                text = year.label(),
                selected = selected,
                onClick = { vault.refine(page.copy(year = if (selected) null else year)) },
            )
        }
    }
}

@Composable
internal fun YearFilter.label(): String = when (this) {
    is YearFilter.In -> year.toString()
    is YearFilter.Before -> stringResource(Res.string.vault_year_before, year)
}

/**
 * DOCUMENTS with the year chips, then the documents under their month
 * headings, [documents] drawing each month's; skeletons while loading. An
 * empty folder says so, with Importer.
 */
@Composable
private fun FolderDocuments(
    vault: Vault,
    page: VaultPage.Folder,
    contents: VaultFolderContents?,
    documents: @Composable (List<FydeDocument>) -> Unit,
) {
    when {
        contents == null -> {
            Spacer(Modifier.height(AppSpacing.space7))
            DocumentRowSkeletons()
        }
        contents.count == 0L && contents.subfolders.isEmpty() -> AppEmptyState(
            modifier = Modifier.padding(top = AppSpacing.space9),
            title = stringResource(Res.string.vault_folder_empty_title),
            description = stringResource(Res.string.vault_folder_empty_description),
            actionLabel = stringResource(Res.string.vault_import).takeIf { vault.canUpload },
            onAction = vault.onUpload.takeIf { vault.canUpload },
        )
        contents.count == 0L -> Unit
        else -> {
            AppSectionTitle(stringResource(Res.string.vault_folder_documents))
            YearChips(vault, page, contents)
            if (contents.documents.isEmpty()) {
                val filtered = page.year != null
                AppEmptyState(
                    modifier = Modifier.padding(top = AppSpacing.space9),
                    title = stringResource(Res.string.no_matches_title),
                    description = stringResource(Res.string.no_documents_match_filters),
                    actionLabel = stringResource(Res.string.clear_filters).takeIf { filtered },
                    onAction = { vault.refine(page.copy(year = null)) }.takeIf { filtered },
                )
            } else {
                val groups = remember(contents.documents) { monthGroups(contents.documents) }
                groups.forEach { group ->
                    MonthHeading(group)
                    documents(group.documents)
                }
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

/** One month's rows, [selected] marked. */
@Composable
private fun FolderRows(vault: Vault, documents: List<FydeDocument>, selected: FydeDocument?) {
    AppGroup {
        documents.forEachIndexed { index, document ->
            if (index > 0) AppDivider()
            VaultDocumentRow(
                document = document,
                onClick = { vault.openDocument(document) },
                selected = document.id == selected?.id,
                onDelete = { vault.askDelete(document) }.takeIf { selected != null },
                onMove = { vault.askMove(document) }.takeIf { selected != null },
            )
        }
    }
}
