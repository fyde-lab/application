package com.peltoche.fyde

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.peltoche.fyde.components.action.AppButton
import com.peltoche.fyde.components.action.AppTextButton
import com.peltoche.fyde.components.content.FolderFillTile
import com.peltoche.fyde.components.form.SourceFolderField
import com.peltoche.fyde.components.overlay.AppBottomSheet
import com.peltoche.fyde.icons.DocumentGlyph
import com.peltoche.fyde.icons.PlusGlyph
import com.peltoche.fyde.icons.SparkleGlyph
import com.peltoche.fyde.layout.AppWindowSizeClass
import com.peltoche.fyde.layout.LocalAppWindowSizeClass
import com.peltoche.fyde.resources.Res
import com.peltoche.fyde.resources.folder_create
import com.peltoche.fyde.resources.source_folder_automatic
import com.peltoche.fyde.resources.source_folder_automatic_help
import com.peltoche.fyde.resources.source_folder_change
import com.peltoche.fyde.resources.source_folder_choose_automatic
import com.peltoche.fyde.resources.source_folder_chosen_help
import com.peltoche.fyde.resources.source_folder_create
import com.peltoche.fyde.resources.source_folder_empty_help
import com.peltoche.fyde.resources.source_folder_into
import com.peltoche.fyde.resources.source_folder_label
import com.peltoche.fyde.resources.source_folder_new_here
import com.peltoche.fyde.resources.source_folder_new_line
import com.peltoche.fyde.resources.source_folder_new_title
import com.peltoche.fyde.resources.source_folder_note
import com.peltoche.fyde.resources.source_folder_same_as
import com.peltoche.fyde.resources.source_folder_stay
import com.peltoche.fyde.resources.source_folder_title
import com.peltoche.fyde.resources.source_folder_title_elided
import com.peltoche.fyde.resources.vault_unclassified
import com.peltoche.fyde.theme.AppColors
import com.peltoche.fyde.theme.AppIconSize
import com.peltoche.fyde.theme.AppShapes
import com.peltoche.fyde.theme.AppSpacing
import com.peltoche.fyde.theme.AppTypography
import kotlinx.coroutines.launch
import org.jetbrains.compose.resources.stringResource

/**
 * Which of a source's folder sheets is open (design-system.md § A source's
 * folder), over the connect form or the account's page that holds it.
 */
sealed interface SourceFolderSheet {
    /** S9 / D12: the picker, Rangement automatique first. */
    data object Pick : SourceFolderSheet

    /**
     * S9b / DO7i: a new folder for the source in [parentId] (`null`: the top
     * level). [fromPicker]: opened from the picker's Nouveau dossier ici, so
     * Annuler goes back to it; from the row (an empty vault), it closes.
     */
    data class Create(val parentId: String?, val fromPicker: Boolean) : SourceFolderSheet
}

/** What creating a folder for a source came to: the [Created] folder, or why it was [Refused], in the SDK's wording. */
sealed interface FolderCreation {
    data class Created(val folder: FydeFolder) : FolderCreation

    data class Refused(val message: String) : FolderCreation
}

/** Publishes a folder in `parentId` from `form`: `App.kt`'s [FydeSdk.createFolder]. */
typealias CreateFolder = suspend (parentId: String?, form: FolderForm) -> FolderCreation

/**
 * The Ranger dans row of a connect form (S3, S3b, S3c, DO7b): [folderId]'s
 * tile and path with Changer, opening S9; Rangement automatique for `null`,
 * or for a folder no longer in the vault. In a vault with no folder yet, À
 * classer with Créer un dossier, opening S9b at the top level. [sameAs]
 * names the account the choice was taken from (S3b), while it's unchanged.
 */
@Composable
internal fun SourceFolderRow(
    folderId: String?,
    onOpen: (SourceFolderSheet) -> Unit,
    enabled: Boolean,
    sameAs: String? = null,
) {
    val folders = LocalVaultFolders.current
    val label = stringResource(Res.string.source_folder_label)
    if (folders.all.isEmpty()) {
        SourceFolderField(
            label = label,
            value = stringResource(Res.string.vault_unclassified),
            actionLabel = stringResource(Res.string.source_folder_create),
            onAction = { onOpen(SourceFolderSheet.Create(parentId = null, fromPicker = false)) },
            tile = {
                FolderFillTile(size = FIELD_TILE, color = null.colors(), shape = AppShapes.iconTile, iconSize = AppIconSize.sm) { tint ->
                    DocumentGlyph(tint = tint)
                }
            },
            supportingText = stringResource(Res.string.source_folder_empty_help),
            actionIcon = { PlusGlyph(modifier = Modifier.size(AppIconSize.sm), tint = AppColors.primary) },
            enabled = enabled,
        )
        return
    }
    val folder = folders[folderId]
    SourceFolderField(
        label = label,
        value = sourceFolderLabel(folderId),
        actionLabel = stringResource(Res.string.source_folder_change),
        onAction = { onOpen(SourceFolderSheet.Pick) },
        tile = { SourceFolderTile(folder, FIELD_TILE, AppIconSize.sm) },
        supportingText = when {
            sameAs != null -> stringResource(Res.string.source_folder_same_as, sameAs)
            folder != null -> stringResource(Res.string.source_folder_chosen_help)
            else -> stringResource(Res.string.source_folder_automatic_help)
        },
        enabled = enabled,
    )
}

/** Where [folderId] files a source's documents, as Réglages and the row read it: the folder's path, or Rangement automatique. */
@Composable
internal fun sourceFolderLabel(folderId: String?): String =
    if (LocalVaultFolders.current[folderId] != null) folderPathLabel(folderId) else stringResource(Res.string.source_folder_automatic)

/** [folder]'s own tile, or Rangement automatique's: the sparkle on a neutral tile. */
@Composable
internal fun SourceFolderTile(folder: FydeFolder?, size: Dp, iconSize: Dp) {
    if (folder != null) {
        FolderFillTile(size = size, color = LocalVaultFolders.current.appearanceOf(folder.id).colors(), shape = AppShapes.iconTile, iconSize = iconSize) { tint ->
            FolderMark(folder, tint)
        }
    } else {
        // `sunk` in the references, drawn as À classer's neutral fill until the tokens migrate.
        FolderFillTile(size = size, color = null.colors(), shape = AppShapes.iconTile, iconSize = iconSize) { tint ->
            SparkleGlyph(tint = tint)
        }
    }
}

/**
 * The open folder [sheet] for one account of a source: S9 / D12 (the picker
 * on [current]'s level, [current] tagged Actuel) or S9b / DO7i (a new folder
 * named after [sourceName], published through [createFolder] then picked).
 * Either way the pick goes to [onChoose] (`null`: Rangement automatique) and
 * the sheets close. [accountName] titles them ("Ameli · Marie").
 */
@Composable
internal fun SourceFolderSheets(
    sheet: SourceFolderSheet?,
    onSheetChange: (SourceFolderSheet?) -> Unit,
    accountName: String,
    sourceName: String,
    current: String?,
    onChoose: (String?) -> Unit,
    createFolder: CreateFolder,
) {
    when (sheet) {
        null -> Unit
        SourceFolderSheet.Pick -> SourceFolderPickSheet(
            accountName = accountName,
            current = current,
            onChoose = {
                onSheetChange(null)
                if (it != current) onChoose(it)
            },
            onNewHere = { level -> onSheetChange(SourceFolderSheet.Create(level, fromPicker = true)) },
            onDismissRequest = { onSheetChange(null) },
        )
        is SourceFolderSheet.Create -> SourceFolderCreateSheet(
            sheet = sheet,
            accountName = accountName,
            sourceName = sourceName,
            createFolder = createFolder,
            onCreated = { folder ->
                onSheetChange(null)
                onChoose(folder.id)
            },
            onDismissRequest = { onSheetChange(SourceFolderSheet.Pick.takeIf { sheet.fromPicker }) },
        )
    }
}

/**
 * S9 / D12: "Ranger les documents d'EDF", that they go into the chosen folder
 * and its subfolders, the picker with Rangement automatique first, that
 * documents already there don't move, then the confirm naming the pick and
 * Nouveau dossier ici (in the level shown) — stacked at Compact, in a row
 * beside it wider.
 */
@Composable
private fun SourceFolderPickSheet(
    accountName: String,
    current: String?,
    onChoose: (String?) -> Unit,
    onNewHere: (level: String?) -> Unit,
    onDismissRequest: () -> Unit,
) {
    val folders = LocalVaultFolders.current
    // A folder deleted since reads as Rangement automatique, which it falls back to.
    val effective = current?.takeIf { folders[it] != null }
    var selected by remember { mutableStateOf<FolderChoice?>(null) }
    var level by remember { mutableStateOf<String?>(null) }
    val confirm = selected.let { choice ->
        when {
            choice == null -> stringResource(Res.string.source_folder_label)
            choice.folderId == null -> stringResource(Res.string.source_folder_choose_automatic)
            else -> stringResource(Res.string.source_folder_into, folders[choice.folderId]?.name.orEmpty())
        }
    }
    AppBottomSheet(onDismissRequest = onDismissRequest) {
        Column(modifier = Modifier.padding(horizontal = AppSpacing.space5), verticalArrangement = Arrangement.spacedBy(AppSpacing.space4)) {
            SheetHeader(sourceFolderTitle(accountName), onDismissRequest)
            Text(text = stringResource(Res.string.source_folder_note), style = AppTypography.caption, color = AppColors.textMuted)
            FolderPicker(
                excluded = emptySet(),
                current = FolderChoice(effective),
                selected = selected,
                onSelect = { selected = it },
                startLevel = folders[effective]?.parentId,
                automaticFiling = true,
                onLevelChange = { level = it },
            )
            Text(text = stringResource(Res.string.source_folder_stay), style = AppTypography.caption, color = AppColors.textMuted)
            val choose = { selected?.let { onChoose(it.folderId) } }
            val newHere: @Composable (Modifier) -> Unit = { modifier ->
                AppTextButton(
                    text = stringResource(Res.string.source_folder_new_here),
                    onClick = { onNewHere(level) },
                    leadingIcon = { PlusGlyph(modifier = Modifier.size(AppIconSize.sm), tint = AppColors.primary) },
                    modifier = modifier,
                )
            }
            if (LocalAppWindowSizeClass.current == AppWindowSizeClass.Compact) {
                AppButton(text = confirm, onClick = { choose() }, enabled = selected != null, modifier = Modifier.fillMaxWidth())
                newHere(Modifier.fillMaxWidth())
            } else {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    newHere(Modifier)
                    Spacer(Modifier.weight(1f))
                    AppButton(text = confirm, onClick = { choose() }, enabled = selected != null)
                }
            }
        }
    }
}

/**
 * S9b / DO7i: V12 / D10 titled "Nouveau dossier pour EDF", saying the source
 * will file all its documents there, its name prefilled with [sourceName].
 * Busy while [createFolder] publishes; a refusal stays in the sheet.
 */
@Composable
private fun SourceFolderCreateSheet(
    sheet: SourceFolderSheet.Create,
    accountName: String,
    sourceName: String,
    createFolder: CreateFolder,
    onCreated: (FydeFolder) -> Unit,
    onDismissRequest: () -> Unit,
) {
    var busy by remember(sheet) { mutableStateOf(false) }
    var error by remember(sheet) { mutableStateOf<String?>(null) }
    val scope = rememberCoroutineScope()
    val appearance = remember(sheet) { randomNewAppearance() }
    FolderFormSheet(
        title = stringResource(Res.string.source_folder_new_title, accountName),
        subtitle = stringResource(Res.string.source_folder_new_line, accountName),
        initial = FolderForm(sourceName.take(FOLDER_NAME_MAX), appearance.takeIf { sheet.parentId == null }),
        parentId = sheet.parentId,
        confirmLabel = stringResource(Res.string.folder_create),
        state = FolderSheetState(FolderSheet.Create(sheet.parentId), busy = busy, error = error),
        onConfirm = { form ->
            busy = true
            error = null
            scope.launch {
                when (val created = createFolder(sheet.parentId, form)) {
                    is FolderCreation.Created -> onCreated(created.folder)
                    is FolderCreation.Refused -> {
                        busy = false
                        error = created.message
                    }
                }
            }
        },
        onDismissRequest = { if (!busy) onDismissRequest() },
    )
}

/** "Ranger les documents d'EDF" / "de Free": French elides "de" before a vowel or a mute h. */
@Composable
private fun sourceFolderTitle(accountName: String): String =
    if (elidesDe(accountName)) {
        stringResource(Res.string.source_folder_title_elided, accountName)
    } else {
        stringResource(Res.string.source_folder_title, accountName)
    }

/** Whether French writes "d'" rather than "de" before [name]. */
internal fun elidesDe(name: String): Boolean = name.firstOrNull()?.lowercaseChar()?.let { it in ELIDING } == true

private const val ELIDING = "aàâäeéèêëiîïoôöuùûüyh"

/** The tile in [SourceFolderField]'s box. */
private val FIELD_TILE = 28.dp
