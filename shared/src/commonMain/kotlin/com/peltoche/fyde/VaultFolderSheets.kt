package com.peltoche.fyde

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.selected
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.peltoche.fyde.components.action.AppButton
import com.peltoche.fyde.components.action.AppButtonVariant
import com.peltoche.fyde.components.action.AppCircleButton
import com.peltoche.fyde.components.action.AppTextButton
import com.peltoche.fyde.components.content.AppGroup
import com.peltoche.fyde.components.content.AppGroupRow
import com.peltoche.fyde.components.content.FolderFillTile
import com.peltoche.fyde.components.feedback.AppErrorNote
import com.peltoche.fyde.components.feedback.AppNote
import com.peltoche.fyde.components.feedback.AppNoteKind
import com.peltoche.fyde.components.form.AppRadio
import com.peltoche.fyde.components.form.AppTextField
import com.peltoche.fyde.components.overlay.AppBottomSheet
import com.peltoche.fyde.components.overlay.AppDialog
import com.peltoche.fyde.icons.ChevronLeftGlyph
import com.peltoche.fyde.icons.ChevronRightGlyph
import com.peltoche.fyde.icons.FolderGlyph
import com.peltoche.fyde.icons.GridGlyph
import com.peltoche.fyde.icons.InfoGlyph
import com.peltoche.fyde.icons.PencilGlyph
import com.peltoche.fyde.icons.PlusGlyph
import com.peltoche.fyde.icons.TrashGlyph
import com.peltoche.fyde.resources.Res
import com.peltoche.fyde.resources.back
import com.peltoche.fyde.resources.cancel
import com.peltoche.fyde.resources.documents_count
import com.peltoche.fyde.resources.filing_unfiled
import com.peltoche.fyde.resources.filing_filed
import com.peltoche.fyde.resources.filing_file
import com.peltoche.fyde.resources.filing_ok
import com.peltoche.fyde.resources.filing_change
import com.peltoche.fyde.resources.document_move_where
import com.peltoche.fyde.resources.document_move_type_note
import com.peltoche.fyde.resources.document_move_title
import com.peltoche.fyde.resources.folder_color
import com.peltoche.fyde.resources.folder_color_n
import com.peltoche.fyde.resources.folder_create
import com.peltoche.fyde.resources.folder_delete
import com.peltoche.fyde.resources.folder_delete_all
import com.peltoche.fyde.resources.folder_delete_all_detail
import com.peltoche.fyde.resources.folder_delete_description_note
import com.peltoche.fyde.resources.folder_delete_empty
import com.peltoche.fyde.resources.folder_delete_holds
import com.peltoche.fyde.resources.folder_delete_keep
import com.peltoche.fyde.resources.folder_delete_keep_detail
import com.peltoche.fyde.resources.folder_delete_title
import com.peltoche.fyde.resources.folder_description
import com.peltoche.fyde.resources.folder_description_help
import com.peltoche.fyde.resources.folder_description_too_long
import com.peltoche.fyde.resources.folder_edit
import com.peltoche.fyde.resources.folder_edit_title
import com.peltoche.fyde.resources.folder_icon
import com.peltoche.fyde.resources.folder_icon_n
import com.peltoche.fyde.resources.folder_location
import com.peltoche.fyde.resources.folder_move
import com.peltoche.fyde.resources.folder_move_into
import com.peltoche.fyde.resources.folder_move_note
import com.peltoche.fyde.resources.folder_move_title
import com.peltoche.fyde.resources.folder_move_to_root
import com.peltoche.fyde.resources.folder_name
import com.peltoche.fyde.resources.folder_name_too_long
import com.peltoche.fyde.resources.folder_new
import com.peltoche.fyde.resources.folder_new_subfolder
import com.peltoche.fyde.resources.folder_picker_current
import com.peltoche.fyde.resources.folder_picker_here
import com.peltoche.fyde.resources.folder_picker_open
import com.peltoche.fyde.resources.folder_picker_root
import com.peltoche.fyde.resources.folder_save
import com.peltoche.fyde.resources.folder_subfolder_color_note
import com.peltoche.fyde.resources.folder_vault_root
import com.peltoche.fyde.resources.subfolders_count
import com.peltoche.fyde.resources.vault_unclassified
import com.peltoche.fyde.theme.AppColors
import com.peltoche.fyde.theme.AppDimensions
import com.peltoche.fyde.theme.AppIconSize
import com.peltoche.fyde.theme.AppShapes
import com.peltoche.fyde.theme.AppSpacing
import com.peltoche.fyde.theme.AppTypography
import org.jetbrains.compose.resources.pluralStringResource
import org.jetbrains.compose.resources.stringResource

/** Which folder sheet or dialog is open over the vault (V12–V15, D7–D10). */
sealed interface FolderSheet {
    /** V13 / D7: the folder's actions. */
    data class Actions(val folderId: String) : FolderSheet

    /** V12 / D10: a new folder in [parentId], `null` for the top level. */
    data class Create(val parentId: String?) : FolderSheet

    /** The folder's name, description and, at the top level, colour and icon. */
    data class Edit(val folderId: String) : FolderSheet

    /** V14 / D8: the folder picker, to move it. */
    data class Move(val folderId: String) : FolderSheet

    /** V15 / D9: deleting it, keeping or deleting what it holds. */
    data class Delete(val folderId: String) : FolderSheet
}

/** The open [sheet], [busy] while its change publishes, [error] why it failed, in the SDK's wording. */
data class FolderSheetState(val sheet: FolderSheet, val busy: Boolean = false, val error: String? = null)

/** What the create and edit forms hand `App.kt`: [appearance] only for a top-level folder. */
data class FolderForm(val name: String, val description: String, val appearance: FydeFolderAppearance?)

/**
 * A document V9 moves, [busy] while the move publishes, [error] why it
 * failed, in the SDK's wording.
 */
data class DocumentMove(val document: FydeDocument, val busy: Boolean = false, val error: String? = null)

/**
 * The folder changes the vault's sheets ask `App.kt` for; each reports back
 * through [VaultUiState.Loaded.folderSheet] (or [VaultUiState.Loaded.documentMove]).
 */
class FolderCallbacks(
    val onSheetChange: (FolderSheetState?) -> Unit = {},
    val onDocumentMoveChange: (DocumentMove?) -> Unit = {},
    val onMoveDocument: (document: FydeDocument, folderId: String?) -> Unit = { _, _ -> },
    /** The filing card's "C'est bon": [document]'s folder is right. */
    val onFilingReviewed: (document: FydeDocument) -> Unit = {},
    val onCreate: (parentId: String?, form: FolderForm) -> Unit = { _, _ -> },
    val onUpdate: (id: String, form: FolderForm) -> Unit = { _, _ -> },
    val onMove: (id: String, parentId: String?) -> Unit = { _, _ -> },
    val onDelete: (id: String, contents: FydeFolderContents) -> Unit = { _, _ -> },
)

/** fyde-sdk's folder rules, checked here only to say so before publishing (the SDK checks again). */
internal const val FOLDER_NAME_MAX = 60
internal const val FOLDER_DESCRIPTION_MAX = 300

/** A new top-level folder's appearance until one is picked. */
private val DefaultAppearance = FydeFolderAppearance(FydeFolderColor.SAND, FydeFolderIcon.FOLDER)

/** Where a folder picker's choice lands: [folderId], or the top level for `null`. */
data class FolderChoice(val folderId: String?)

/** The open folder sheet, drawn over whichever vault page asked for it. */
@Composable
internal fun FolderSheets(vault: Vault) {
    val state = vault.state.folderSheet ?: return
    val folders = LocalVaultFolders.current
    val callbacks = vault.folderCallbacks
    fun open(sheet: FolderSheet) = callbacks.onSheetChange(FolderSheetState(sheet))
    val close = { callbacks.onSheetChange(null) }
    val sheet = state.sheet
    val folderId = when (sheet) {
        is FolderSheet.Create -> null
        is FolderSheet.Actions -> sheet.folderId
        is FolderSheet.Edit -> sheet.folderId
        is FolderSheet.Move -> sheet.folderId
        is FolderSheet.Delete -> sheet.folderId
    }
    val folder = folders[folderId]
    // The folder was deleted meanwhile, here or on another device.
    if (folderId != null && folder == null) {
        LaunchedEffect(folderId) { close() }
        return
    }
    when (sheet) {
        is FolderSheet.Actions -> FolderActionsSheet(folder!!, onOpen = ::open, onDismissRequest = close)
        is FolderSheet.Create -> FolderFormSheet(
            title = stringResource(Res.string.folder_new),
            initial = FolderForm("", "", DefaultAppearance.takeIf { sheet.parentId == null }),
            parentId = sheet.parentId,
            confirmLabel = stringResource(Res.string.folder_create),
            state = state,
            onConfirm = { callbacks.onCreate(sheet.parentId, it) },
            onDismissRequest = close,
        )
        is FolderSheet.Edit -> FolderFormSheet(
            title = stringResource(Res.string.folder_edit_title),
            initial = FolderForm(folder!!.name, folder.description, folder.appearance ?: DefaultAppearance.takeIf { folder.parentId == null }),
            parentId = folder.parentId,
            confirmLabel = stringResource(Res.string.folder_save),
            state = state,
            onConfirm = { callbacks.onUpdate(folder.id, it) },
            onDismissRequest = close,
        )
        is FolderSheet.Move -> FolderMoveSheet(folder!!, state, onMove = { callbacks.onMove(folder.id, it) }, onDismissRequest = close)
        is FolderSheet.Delete -> DeleteFolderDialog(
            folder = folder!!,
            documentCount = vault.state.home?.counts?.get(folder.id) ?: 0,
            state = state,
            onDelete = { callbacks.onDelete(folder.id, it) },
            onDismissRequest = close,
        )
    }
}

/** A sheet's title with Annuler beside it. */
@Composable
private fun SheetHeader(title: String, onCancel: () -> Unit) {
    Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth()) {
        Text(text = title, style = AppTypography.section, color = AppColors.text, modifier = Modifier.weight(1f))
        AppTextButton(text = stringResource(Res.string.cancel), onClick = onCancel)
    }
}

/** [folder]'s mark on its top-level folder's fill. */
@Composable
internal fun SheetFolderTile(folder: FydeFolder, size: androidx.compose.ui.unit.Dp = AppDimensions.iconTile) {
    val folders = LocalVaultFolders.current
    FolderFillTile(size = size, color = folders.appearanceOf(folder.id).colors(), shape = AppShapes.iconTile, iconSize = AppIconSize.md) { tint ->
        FolderMark(folder, tint)
    }
}

/** "Coffre › Logement": where a folder in [parentId] is, the vault itself for the top level. */
@Composable
private fun locationLabel(parentId: String?): String {
    val root = stringResource(Res.string.folder_vault_root)
    val path = LocalVaultFolders.current.path(parentId)
    return (listOf(root) + path.map { it.name }).joinToString(FOLDER_PATH_SEPARATOR)
}

/**
 * V13 / D7: the folder's tile, name and place over its actions — a
 * subfolder, renaming and describing it (and its colour at the top level),
 * moving it, then deleting it apart in `error`.
 */
@Composable
private fun FolderActionsSheet(folder: FydeFolder, onOpen: (FolderSheet) -> Unit, onDismissRequest: () -> Unit) {
    AppBottomSheet(onDismissRequest = onDismissRequest) {
        Column(modifier = Modifier.padding(horizontal = AppSpacing.space5)) {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(AppSpacing.space4)) {
                SheetFolderTile(folder)
                Column(modifier = Modifier.weight(1f)) {
                    Text(text = folder.name, style = AppTypography.bodyEmphasis, color = AppColors.text, maxLines = 2, overflow = TextOverflow.Ellipsis)
                    Text(text = locationLabel(folder.parentId), style = AppTypography.caption, color = AppColors.textMuted)
                }
            }
            Spacer(Modifier.height(AppSpacing.space5))
            AppGroup {
                ActionRow(stringResource(Res.string.folder_new_subfolder), { PlusGlyph(Modifier.size(AppIconSize.md), AppColors.text) }) {
                    onOpen(FolderSheet.Create(folder.id))
                }
                ActionRow(
                    label = stringResource(Res.string.folder_edit),
                    icon = { PencilGlyph(Modifier.size(AppIconSize.md), AppColors.text) },
                    detail = folder.description.lineSequence().firstOrNull()?.takeIf { it.isNotBlank() },
                ) {
                    onOpen(FolderSheet.Edit(folder.id))
                }
                ActionRow(
                    label = stringResource(Res.string.folder_move),
                    icon = { FolderGlyph(Modifier.size(AppIconSize.md), AppColors.text) },
                    detail = locationLabel(folder.parentId),
                ) {
                    onOpen(FolderSheet.Move(folder.id))
                }
            }
            Spacer(Modifier.height(AppSpacing.space4))
            AppGroup {
                AppGroupRow(onClick = { onOpen(FolderSheet.Delete(folder.id)) }) {
                    TrashGlyph(modifier = Modifier.size(AppIconSize.md), tint = AppColors.error)
                    Text(stringResource(Res.string.folder_delete), style = AppTypography.body, color = AppColors.error)
                }
            }
        }
    }
}

@Composable
private fun ActionRow(label: String, icon: @Composable () -> Unit, detail: String? = null, onClick: () -> Unit) {
    AppGroupRow(onClick = onClick) {
        icon()
        Column(modifier = Modifier.weight(1f)) {
            Text(label, style = AppTypography.body, color = AppColors.text)
            if (detail != null) {
                Text(detail, style = AppTypography.caption, color = AppColors.textMuted, maxLines = 1, overflow = TextOverflow.Ellipsis)
            }
        }
    }
}

/**
 * V12 / D10, and editing a folder: its name (with the tile it will have),
 * its description with what Fyde does with it, where it is, and for a
 * top-level folder its colour and icon. A subfolder picks neither.
 */
@Composable
private fun FolderFormSheet(
    title: String,
    initial: FolderForm,
    parentId: String?,
    confirmLabel: String,
    state: FolderSheetState,
    onConfirm: (FolderForm) -> Unit,
    onDismissRequest: () -> Unit,
) {
    var name by remember(state.sheet) { mutableStateOf(initial.name) }
    var description by remember(state.sheet) { mutableStateOf(initial.description) }
    var appearance by remember(state.sheet) { mutableStateOf(initial.appearance) }
    val topLevel = parentId == null
    val nameTooLong = name.trim().length > FOLDER_NAME_MAX
    val descriptionTooLong = description.trim().length > FOLDER_DESCRIPTION_MAX
    val valid = name.isNotBlank() && !nameTooLong && !descriptionTooLong
    val tileAppearance = if (topLevel) appearance else LocalVaultFolders.current.appearanceOf(parentId)
    AppBottomSheet(onDismissRequest = onDismissRequest) {
        Column(modifier = Modifier.padding(horizontal = AppSpacing.space5), verticalArrangement = Arrangement.spacedBy(AppSpacing.space4)) {
            SheetHeader(title, onDismissRequest)
            AppTextField(
                value = name,
                onValueChange = { name = it },
                label = stringResource(Res.string.folder_name),
                errorText = stringResource(Res.string.folder_name_too_long).takeIf { nameTooLong },
                enabled = !state.busy,
                leadingIcon = {
                    FolderFillTile(size = 28.dp, color = tileAppearance.colors(), shape = AppShapes.iconTile, iconSize = AppIconSize.sm) { tint ->
                        if (topLevel) FolderIconGlyph(appearance?.icon, tint) else FolderGlyph(tint = tint)
                    }
                },
            )
            AppTextField(
                value = description,
                onValueChange = { description = it },
                label = stringResource(Res.string.folder_description),
                supportingText = stringResource(Res.string.folder_description_help),
                errorText = stringResource(Res.string.folder_description_too_long).takeIf { descriptionTooLong },
                enabled = !state.busy,
                singleLine = false,
            )
            Text(
                text = stringResource(Res.string.folder_location, locationLabel(parentId)),
                style = AppTypography.caption,
                color = AppColors.textMuted,
            )
            if (topLevel) {
                AppearancePicker(appearance ?: DefaultAppearance, onChange = { appearance = it })
            } else {
                Text(text = stringResource(Res.string.folder_subfolder_color_note), style = AppTypography.caption, color = AppColors.textMuted)
            }
            state.error?.let { AppErrorNote(it) }
            AppButton(
                text = confirmLabel,
                onClick = { onConfirm(FolderForm(name.trim(), description.trim(), appearance.takeIf { topLevel })) },
                enabled = valid,
                loading = state.busy,
                modifier = Modifier.fillMaxWidth(),
            )
        }
    }
}

/** COULEUR's swatches then ICÔNE's cells, each a single choice. */
@Composable
private fun AppearancePicker(appearance: FydeFolderAppearance, onChange: (FydeFolderAppearance) -> Unit) {
    val colors = FydeFolderColor.entries
    val icons = FydeFolderIcon.entries
    Text(text = stringResource(Res.string.folder_color).uppercase(), style = AppTypography.overline, color = AppColors.textMuted)
    FlowRow(horizontalArrangement = Arrangement.spacedBy(AppSpacing.space3), verticalArrangement = Arrangement.spacedBy(AppSpacing.space3)) {
        colors.forEachIndexed { index, color ->
            val swatch = FydeFolderAppearance(color, appearance.icon)
            PickerCell(
                selected = color == appearance.color,
                description = stringResource(Res.string.folder_color_n, index + 1, colors.size),
                fill = swatch.colors().fill,
                onClick = { onChange(swatch) },
            ) {}
        }
    }
    Text(text = stringResource(Res.string.folder_icon).uppercase(), style = AppTypography.overline, color = AppColors.textMuted)
    FlowRow(horizontalArrangement = Arrangement.spacedBy(AppSpacing.space3), verticalArrangement = Arrangement.spacedBy(AppSpacing.space3)) {
        val colors = appearance.colors()
        icons.forEachIndexed { index, icon ->
            val selected = icon == appearance.icon
            PickerCell(
                selected = selected,
                description = stringResource(Res.string.folder_icon_n, index + 1, icons.size),
                fill = if (selected) colors.fill else AppColors.surface,
                onClick = { onChange(appearance.copy(icon = icon)) },
            ) {
                FolderIconGlyph(icon, tint = if (selected) colors.ink else AppColors.text, modifier = Modifier.size(AppIconSize.md))
            }
        }
    }
}

/** One swatch or icon: a 40dp tile, ringed in `primary` when picked. */
@Composable
private fun PickerCell(
    selected: Boolean,
    description: String,
    fill: androidx.compose.ui.graphics.Color,
    onClick: () -> Unit,
    content: @Composable () -> Unit,
) {
    Box(
        modifier = Modifier
            .size(40.dp)
            .clip(AppShapes.iconTile)
            .background(fill)
            .border(
                width = if (selected) 2.dp else AppDimensions.borderHairline,
                color = if (selected) AppColors.primary else AppColors.border,
                shape = AppShapes.iconTile,
            )
            .clickable(role = Role.RadioButton, onClick = onClick)
            .semantics {
                contentDescription = description
                this.selected = selected
            },
        contentAlignment = Alignment.Center,
    ) { content() }
}

/**
 * The folder picker (V14, and V9 for a document), browsable by level:
 * at the top "À la racine du coffre", in a folder "Dans <folder> même",
 * then the level's folders, each with a chevron into its own. [excluded]
 * folders are left out (a moved folder and what's below it); [current] is
 * tagged Actuel and can't be picked.
 */
@Composable
internal fun FolderPicker(
    excluded: Set<String>,
    current: FolderChoice,
    selected: FolderChoice?,
    onSelect: (FolderChoice) -> Unit,
    startLevel: String? = null,
) {
    val folders = LocalVaultFolders.current
    var level by remember { mutableStateOf(startLevel?.takeIf { it !in excluded && folders[it] != null }) }
    val levelFolder = folders[level]
    Column(verticalArrangement = Arrangement.spacedBy(AppSpacing.space3)) {
        if (levelFolder != null) {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(AppSpacing.space3)) {
                AppCircleButton(onClick = { level = levelFolder.parentId }, contentDescription = stringResource(Res.string.back)) {
                    ChevronLeftGlyph(modifier = Modifier.size(AppIconSize.sm), tint = AppColors.text)
                }
                Text(text = locationLabel(levelFolder.id), style = AppTypography.caption, color = AppColors.textMuted)
            }
        }
        AppGroup {
            val here = FolderChoice(level)
            PickerRow(
                label = if (levelFolder == null) {
                    stringResource(Res.string.folder_picker_root)
                } else {
                    stringResource(Res.string.folder_picker_here, levelFolder.name)
                },
                tile = {
                    if (levelFolder != null) {
                        SheetFolderTile(levelFolder)
                    } else {
                        FolderFillTile(size = AppDimensions.iconTile, color = null.colors(), shape = AppShapes.iconTile, iconSize = AppIconSize.md) { tint ->
                            GridGlyph(tint = tint)
                        }
                    }
                },
                current = here == current,
                selected = here == selected,
                onSelect = { onSelect(here) },
                onOpen = null,
            )
            folders.children(level).filter { it.id !in excluded }.forEach { folder ->
                val choice = FolderChoice(folder.id)
                val canOpen = folders.children(folder.id).any { it.id !in excluded }
                PickerRow(
                    label = folder.name,
                    tile = { SheetFolderTile(folder) },
                    current = choice == current,
                    selected = choice == selected,
                    onSelect = { onSelect(choice) },
                    onOpen = { level = folder.id }.takeIf { canOpen },
                )
            }
        }
    }
}

@Composable
private fun PickerRow(
    label: String,
    tile: @Composable () -> Unit,
    current: Boolean,
    selected: Boolean,
    onSelect: () -> Unit,
    onOpen: (() -> Unit)?,
) {
    AppGroupRow(onClick = onSelect.takeIf { !current }, selected = selected, enabled = !current) {
        tile()
        Text(
            text = label,
            style = AppTypography.body,
            color = if (current) AppColors.textMuted else AppColors.text,
            modifier = Modifier.weight(1f),
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
        )
        if (current) {
            Text(text = stringResource(Res.string.folder_picker_current), style = AppTypography.caption, color = AppColors.textMuted)
        }
        if (selected) AppRadio(selected = true)
        if (onOpen != null) {
            AppCircleButton(onClick = onOpen, contentDescription = stringResource(Res.string.folder_picker_open, label)) {
                ChevronRightGlyph(modifier = Modifier.size(AppIconSize.sm), tint = AppColors.textMuted)
            }
        }
    }
}

/** The confirm label for moving into [choice]: "Déplacer dans Énergie", to the top level, or "Déplacer" before a pick. */
@Composable
internal fun moveIntoLabel(choice: FolderChoice?): String {
    val folder = LocalVaultFolders.current[choice?.folderId]
    return when {
        choice == null -> stringResource(Res.string.folder_move)
        folder != null -> stringResource(Res.string.folder_move_into, folder.name)
        else -> stringResource(Res.string.folder_move_to_root)
    }
}

/** V14 / D8: what moves with the folder, the picker without it and what's below, then Déplacer. */
@Composable
private fun FolderMoveSheet(folder: FydeFolder, state: FolderSheetState, onMove: (String?) -> Unit, onDismissRequest: () -> Unit) {
    val folders = LocalVaultFolders.current
    val excluded = remember(folders, folder.id) { folders.withDescendants(folder.id) }
    var selected by remember(folder.id) { mutableStateOf<FolderChoice?>(null) }
    AppBottomSheet(onDismissRequest = onDismissRequest) {
        Column(modifier = Modifier.padding(horizontal = AppSpacing.space5), verticalArrangement = Arrangement.spacedBy(AppSpacing.space4)) {
            SheetHeader(stringResource(Res.string.folder_move_title, folder.name), onDismissRequest)
            Text(text = stringResource(Res.string.folder_move_note), style = AppTypography.caption, color = AppColors.textMuted)
            FolderPicker(
                excluded = excluded,
                current = FolderChoice(folder.parentId),
                selected = selected,
                onSelect = { selected = it },
            )
            state.error?.let { AppErrorNote(it) }
            AppButton(
                text = moveIntoLabel(selected),
                onClick = { selected?.let { onMove(it.folderId) } },
                enabled = selected != null,
                loading = state.busy,
                modifier = Modifier.fillMaxWidth(),
            )
        }
    }
}

/**
 * V15 / D9: an empty folder asks a plain confirmation. Otherwise it says
 * what the folder holds and offers Les garder (moved up to its parent, or
 * À classer from the top level; the default) or Les supprimer aussi (from
 * every device), and where documents matching its description will go.
 */
@Composable
private fun DeleteFolderDialog(
    folder: FydeFolder,
    documentCount: Long,
    state: FolderSheetState,
    onDelete: (FydeFolderContents) -> Unit,
    onDismissRequest: () -> Unit,
) {
    val folders = LocalVaultFolders.current
    val subfolders = folders.children(folder.id).size
    val empty = subfolders == 0 && documentCount == 0L
    var contents by remember(folder.id) { mutableStateOf(FydeFolderContents.KEEP) }
    val parentLabel = folders[folder.parentId]?.name ?: stringResource(Res.string.vault_unclassified)
    val holds = listOfNotNull(
        pluralStringResource(Res.plurals.subfolders_count, subfolders, subfolders).takeIf { subfolders > 0 },
        pluralStringResource(Res.plurals.documents_count, documentCount.toInt(), documentCount.toInt()).takeIf { documentCount > 0 },
    ).joinToString(" · ")
    AppDialog(
        onDismissRequest = { if (!state.busy) onDismissRequest() },
        title = stringResource(Res.string.folder_delete_title, folder.name),
        text = if (empty) stringResource(Res.string.folder_delete_empty) else stringResource(Res.string.folder_delete_holds, holds),
        confirmLabel = stringResource(Res.string.folder_delete),
        onConfirm = { onDelete(contents) },
        dismissLabel = stringResource(Res.string.cancel),
        onDismiss = onDismissRequest,
        destructive = true,
        confirmLoading = state.busy,
        focusDismiss = true,
    ) {
        Column(verticalArrangement = Arrangement.spacedBy(AppSpacing.space3)) {
            if (!empty) {
                AppGroup {
                    ContentsChoice(
                        label = stringResource(Res.string.folder_delete_keep),
                        detail = stringResource(Res.string.folder_delete_keep_detail, parentLabel),
                        selected = contents == FydeFolderContents.KEEP,
                        onClick = { contents = FydeFolderContents.KEEP },
                    )
                    ContentsChoice(
                        label = stringResource(Res.string.folder_delete_all),
                        detail = stringResource(Res.string.folder_delete_all_detail),
                        selected = contents == FydeFolderContents.DELETE,
                        onClick = { contents = FydeFolderContents.DELETE },
                    )
                }
            }
            if (folder.description.isNotBlank()) {
                AppNote(
                    message = stringResource(Res.string.folder_delete_description_note),
                    kind = AppNoteKind.Info,
                    icon = { tint -> InfoGlyph(modifier = Modifier.size(AppIconSize.sm), tint = tint) },
                )
            }
            state.error?.let { AppErrorNote(it) }
        }
    }
}

@Composable
private fun ContentsChoice(label: String, detail: String, selected: Boolean, onClick: () -> Unit) {
    AppGroupRow(onClick = onClick, selected = selected) {
        AppRadio(selected = selected)
        Column(modifier = Modifier.weight(1f)) {
            Text(label, style = AppTypography.body, color = AppColors.text)
            Text(detail, style = AppTypography.caption, color = AppColors.textMuted)
        }
    }
}

/**
 * V9: where the document is, the folder picker on its folder's level (its
 * folder tagged Actuel), that its type doesn't change, then Déplacer.
 */
@Composable
internal fun MoveDocumentSheet(vault: Vault) {
    val move = vault.state.documentMove ?: return
    val document = move.document
    val folders = LocalVaultFolders.current
    val callbacks = vault.folderCallbacks
    var selected by remember(document.id) { mutableStateOf<FolderChoice?>(null) }
    val close = { callbacks.onDocumentMoveChange(null) }
    AppBottomSheet(onDismissRequest = { if (!move.busy) close() }) {
        Column(modifier = Modifier.padding(horizontal = AppSpacing.space5), verticalArrangement = Arrangement.spacedBy(AppSpacing.space4)) {
            SheetHeader(stringResource(Res.string.document_move_title), close)
            Text(
                text = stringResource(Res.string.document_move_where, document.name, document.folderLabel()),
                style = AppTypography.caption,
                color = AppColors.textMuted,
            )
            FolderPicker(
                excluded = emptySet(),
                current = FolderChoice(document.folderId),
                selected = selected,
                onSelect = { selected = it },
                startLevel = folders[document.folderId]?.parentId,
            )
            document.documentType?.let {
                Text(text = stringResource(Res.string.document_move_type_note, it.label()), style = AppTypography.caption, color = AppColors.textMuted)
            }
            move.error?.let { AppErrorNote(it) }
            AppButton(
                text = moveIntoLabel(selected),
                onClick = { selected?.let { callbacks.onMoveDocument(document, it.folderId) } },
                enabled = selected != null,
                loading = move.busy,
                modifier = Modifier.fillMaxWidth(),
            )
        }
    }
}

/**
 * Under a document's pass: the folder Fyde put it in ("Rangé dans Travail ›
 * Bulletins de paie") with Modifier, or "Dans À classer" with Ranger — both
 * opening V9 — and, until the user confirmed it ([onReviewed]), C'est bon.
 */
@Composable
internal fun FilingCard(document: FydeDocument, onMove: () -> Unit, onReviewed: (() -> Unit)? = null) {
    val folders = LocalVaultFolders.current
    val filed = folders[document.folderId] != null
    // À classer has nothing to confirm: Ranger is the only way on.
    val asking = onReviewed != null && filed
    val change = stringResource(if (filed) Res.string.filing_change else Res.string.filing_file)
    AppGroup {
        AppGroupRow {
            FolderFillTile(size = AppDimensions.iconTile, color = document.colors(), shape = AppShapes.iconTile, iconSize = AppIconSize.md) { tint ->
                DocumentFolderGlyph(document, tint)
            }
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = stringResource(if (filed) Res.string.filing_filed else Res.string.filing_unfiled),
                    style = AppTypography.caption,
                    color = AppColors.textMuted,
                )
                if (filed) {
                    Text(text = document.folderLabel(), style = AppTypography.bodyEmphasis, color = AppColors.text)
                }
            }
            if (!asking) AppTextButton(text = change, onClick = onMove)
        }
        // Asking, the two answers get their own line under the folder.
        if (asking) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(start = AppSpacing.space5, end = AppSpacing.space5, bottom = AppSpacing.space4),
                horizontalArrangement = Arrangement.spacedBy(AppSpacing.space3, Alignment.End),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                AppTextButton(text = change, onClick = onMove)
                AppButton(text = stringResource(Res.string.filing_ok), onClick = onReviewed!!, variant = AppButtonVariant.Tonal)
            }
        }
    }
}
