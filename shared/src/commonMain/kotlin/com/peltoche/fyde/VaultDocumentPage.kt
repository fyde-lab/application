package com.peltoche.fyde

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Text
import androidx.compose.material3.ripple
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import com.peltoche.fyde.components.action.AppButton
import com.peltoche.fyde.components.action.AppButtonVariant
import com.peltoche.fyde.components.action.AppCircleButton
import com.peltoche.fyde.components.content.AppDivider
import com.peltoche.fyde.components.content.AppGroup
import com.peltoche.fyde.components.content.AppGroupRow
import com.peltoche.fyde.components.content.AppIconTile
import com.peltoche.fyde.components.content.DocumentAttributes
import com.peltoche.fyde.components.content.DocumentPass
import com.peltoche.fyde.components.content.DocumentPage
import com.peltoche.fyde.components.feedback.AppErrorNote
import com.peltoche.fyde.components.navigation.AppPageBar
import com.peltoche.fyde.components.navigation.SystemBackHandler
import com.peltoche.fyde.components.overlay.AppBottomSheet
import com.peltoche.fyde.components.overlay.AppDialog
import com.peltoche.fyde.components.overlay.AppDropdownMenu
import com.peltoche.fyde.components.overlay.AppDropdownMenuItem
import com.peltoche.fyde.icons.ChevronRightGlyph
import com.peltoche.fyde.icons.CloseGlyph
import com.peltoche.fyde.icons.DownloadGlyph
import com.peltoche.fyde.icons.LockGlyph
import com.peltoche.fyde.icons.FolderGlyph
import com.peltoche.fyde.icons.MoreGlyph
import com.peltoche.fyde.icons.PagesGlyph
import com.peltoche.fyde.icons.TrashGlyph
import com.peltoche.fyde.resources.Res
import com.peltoche.fyde.resources.cancel
import com.peltoche.fyde.resources.document_move
import com.peltoche.fyde.resources.document_move_shortcut
import com.peltoche.fyde.resources.documents_delete_text
import com.peltoche.fyde.resources.documents_delete_text_devices
import com.peltoche.fyde.resources.documents_delete_title
import com.peltoche.fyde.resources.documents_more
import com.peltoche.fyde.resources.vault_delete
import com.peltoche.fyde.resources.vault_delete_confirm
import com.peltoche.fyde.resources.vault_delete_shortcut
import com.peltoche.fyde.resources.vault_delete_text
import com.peltoche.fyde.resources.vault_delete_text_devices
import com.peltoche.fyde.resources.vault_delete_title
import com.peltoche.fyde.resources.vault_document_actions
import com.peltoche.fyde.resources.vault_fact_arrived
import com.peltoche.fyde.resources.vault_fact_encrypted
import com.peltoche.fyde.resources.vault_fact_encrypted_devices
import com.peltoche.fyde.resources.vault_info_heading
import com.peltoche.fyde.resources.vault_fact_in_vault
import com.peltoche.fyde.resources.vault_file_caption
import com.peltoche.fyde.resources.vault_save
import com.peltoche.fyde.resources.vault_viewer_close
import com.peltoche.fyde.resources.vault_viewer_decrypted
import com.peltoche.fyde.theme.AppColors
import com.peltoche.fyde.theme.AppDimensions
import com.peltoche.fyde.theme.AppIconSize
import com.peltoche.fyde.theme.AppRadius
import com.peltoche.fyde.theme.AppSpacing
import com.peltoche.fyde.theme.AppTypography
import com.peltoche.fyde.theme.appFocusRing
import org.jetbrains.compose.resources.pluralStringResource
import org.jetbrains.compose.resources.stringResource

/**
 * V6: a document's pass, its file (opening the viewer), its facts, and
 * Save; "…" opens V8.
 */
@Composable
internal fun DocumentPage(vault: Vault, document: FydeDocument) {
    var actionsOpen by remember { mutableStateOf(false) }
    Column(modifier = Modifier.fillMaxSize().verticalScroll(rememberScrollState())) {
        AppPageBar(
            title = document.folderLabel(),
            onBack = vault::closeDocument,
            trailing = {
                AppCircleButton(
                    onClick = { actionsOpen = true },
                    contentDescription = stringResource(Res.string.vault_document_actions),
                    active = actionsOpen,
                ) {
                    MoreGlyph(modifier = Modifier.size(AppIconSize.md), tint = AppColors.text)
                }
            },
        )
        VaultNotes(vault)
        Spacer(Modifier.height(AppSpacing.space3))
        DocumentDetails(
            vault = vault,
            document = document,
            onOpenFile = { vault.onFocusChange(vault.focus.copy(viewerOpen = true)) },
        )
        Spacer(Modifier.height(AppSpacing.space7))
    }
    if (actionsOpen) {
        DocumentActionsSheet(
            document = document,
            onSave = { vault.onSave(document) },
            onDelete = { vault.askDelete(document) },
            onMove = { vault.askMove(document) },
            onDismissRequest = { actionsOpen = false },
        )
    }
}

/**
 * D2's right side: the document's pass column, then its content previewed
 * in the rest of the window. [onBack] heads the column when nothing else
 * (D2's list column) leads back.
 */
@Composable
internal fun RowScope.DocumentBeside(vault: Vault, document: FydeDocument, onBack: (() -> Unit)?) {
    Box(
        modifier = Modifier
            .width(AppDimensions.documentPassColumn)
            .fillMaxHeight()
            .background(AppColors.surface),
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = AppSpacing.space5),
        ) {
            if (onBack != null) {
                AppPageBar(title = document.folderLabel(), onBack = onBack)
                VaultNotes(vault)
            }
            Spacer(Modifier.height(AppSpacing.space5))
            DocumentDetails(vault = vault, document = document, onOpenFile = null)
            Spacer(Modifier.height(AppSpacing.space7))
        }
    }
    AppDivider(vertical = true)
    DocumentPreview(
        document = document,
        loadContent = vault.loadContent,
        modifier = Modifier.weight(1f).fillMaxHeight().padding(vertical = AppSpacing.space5),
    )
}

/**
 * V7: the document's content filling the page, under a bar to close it, the
 * reminder that it was decrypted here at the bottom.
 */
@Composable
internal fun DocumentViewer(
    document: FydeDocument,
    loadContent: suspend (FydeDocument) -> Result<ByteArray?>,
    onClose: () -> Unit,
) {
    SystemBackHandler(onClose)
    Column(modifier = Modifier.fillMaxSize()) {
        Row(
            modifier = Modifier.fillMaxWidth().heightIn(min = AppDimensions.topBarHeight),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            AppCircleButton(onClick = onClose, contentDescription = stringResource(Res.string.vault_viewer_close)) {
                CloseGlyph(modifier = Modifier.size(AppIconSize.md), tint = AppColors.text)
            }
            Spacer(Modifier.width(AppSpacing.space4))
            Text(
                text = document.name,
                style = AppTypography.bodyEmphasis,
                color = AppColors.text,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.weight(1f),
            )
        }
        DocumentPreview(document = document, loadContent = loadContent, modifier = Modifier.weight(1f).fillMaxWidth())
        Row(
            modifier = Modifier.fillMaxWidth().padding(vertical = AppSpacing.space4),
            horizontalArrangement = Arrangement.Center,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            LockGlyph(modifier = Modifier.size(AppIconSize.sm))
            Spacer(Modifier.width(AppSpacing.space2))
            Text(
                text = stringResource(Res.string.vault_viewer_decrypted),
                style = AppTypography.caption,
                color = AppColors.textMuted,
                textAlign = TextAlign.Center,
            )
        }
    }
}

/**
 * The pass, the file row (when [onOpenFile] opens the content elsewhere;
 * D2 previews it beside), Informations, the facts, and Save. The pass's
 * template and Informations' rows come from the document's attributes
 * (design-system.md § Document pass, § Informations).
 */
@Composable
private fun DocumentDetails(vault: Vault, document: FydeDocument, onOpenFile: (() -> Unit)?) {
    val size = localizedSize(document.size)
    val layout = remember(document) { passLayout(document) }
    val information = remember(document) { informationRows(document) }
    val pass = passText(document, layout, fallbackIssuer = LocalVaultFolders.current.topLevelOf(document.folderId)?.name ?: folderPathLabel(null))
    val type = document.documentType?.label()
    Column {
        DocumentPass(
            issuer = pass.issuer,
            type = type,
            headline = pass.headline,
            line = pass.line,
            cells = pass.cells,
            color = document.colors(),
            headlineKind = pass.headlineKind,
            description = listOfNotNull(listOfNotNull(type, pass.issuer).joinToString(" "), pass.headline, pass.line)
                .plus(pass.cells.map { "${it.label} ${it.value}" })
                .filter { it.isNotBlank() }
                .joinToString(", "),
            icon = { tint -> DocumentFolderGlyph(document, tint) },
        )
        Spacer(Modifier.height(AppSpacing.space4))
        FilingCard(document, onMove = { vault.askMove(document) })
        if (onOpenFile != null) {
            Spacer(Modifier.height(AppSpacing.space5))
            AppGroup { FileRow(document, size, onOpenFile) }
        }
        if (information.isNotEmpty()) {
            Spacer(Modifier.height(AppSpacing.space5))
            val feminine = isFeminine(document.documentType)
            DocumentAttributes(
                heading = stringResource(Res.string.vault_info_heading),
                rows = information.map { informationText(it, feminine) },
            )
        }
        Spacer(Modifier.height(AppSpacing.space5))
        AppGroup {
            FactRow(stringResource(Res.string.vault_fact_arrived), localizedDate(document.createdAt))
            AppDivider()
            val devices = vault.state.deviceCount
            FactRow(
                stringResource(Res.string.vault_fact_in_vault),
                if (devices != null) {
                    pluralStringResource(Res.plurals.vault_fact_encrypted_devices, devices, devices)
                } else {
                    stringResource(Res.string.vault_fact_encrypted)
                },
            )
        }
        Spacer(Modifier.height(AppSpacing.space5))
        Row(
            horizontalArrangement = Arrangement.spacedBy(AppSpacing.space3),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            AppButton(
                text = stringResource(Res.string.vault_save),
                onClick = { vault.onSave(document) },
                modifier = Modifier.weight(1f),
                leadingIcon = { DownloadGlyph(modifier = Modifier.size(AppIconSize.sm), tint = AppColors.onPrimary) },
            )
            // Below Expanded the page bar's "…" holds these (V8).
            if (onOpenFile == null) {
                AppButton(
                    text = stringResource(Res.string.document_move),
                    onClick = { vault.askMove(document) },
                    variant = AppButtonVariant.Outlined,
                )
                DocumentActionsButton(onDelete = { vault.askDelete(document) }, onMove = { vault.askMove(document) })
            }
        }
        // D2 keeps the document open beside the similar list (D11); below, the list takes the page.
        SimilarSection(vault, document, keepOpen = onOpenFile == null)
    }
}

/** D2b: "…" beside Save, opening the document's actions. */
@Composable
private fun DocumentActionsButton(onDelete: () -> Unit, onMove: () -> Unit) {
    var open by remember { mutableStateOf(false) }
    Box {
        AppCircleButton(
            onClick = { open = true },
            contentDescription = stringResource(Res.string.vault_document_actions),
            active = open,
        ) {
            MoreGlyph(modifier = Modifier.size(AppIconSize.md), tint = if (open) AppColors.primary else AppColors.text)
        }
        DocumentActionsMenu(expanded = open, onDismissRequest = { open = false }, onDelete = onDelete, onMove = onMove)
    }
}

/**
 * D2b's menu, from "…" or a right-click on a D2 row: what the SDK backs of
 * V8 that the page doesn't already show, i.e. deleting.
 */
@Composable
internal fun DocumentActionsMenu(expanded: Boolean, onDismissRequest: () -> Unit, onDelete: () -> Unit, onMove: (() -> Unit)? = null) {
    val platform = getPlatform()
    AppDropdownMenu(expanded = expanded, onDismissRequest = onDismissRequest) {
        if (onMove != null) {
            AppDropdownMenuItem(
                text = stringResource(Res.string.document_move),
                onClick = {
                    onDismissRequest()
                    onMove()
                },
                leadingIcon = { tint -> FolderGlyph(modifier = Modifier.size(AppIconSize.md), tint = tint) },
                shortcut = when {
                    !platform.isDesktop -> null
                    platform.usesCommandKey -> "⇧⌘M"
                    else -> stringResource(Res.string.document_move_shortcut)
                },
            )
        }
        AppDropdownMenuItem(
            text = stringResource(Res.string.vault_delete),
            onClick = {
                onDismissRequest()
                onDelete()
            },
            destructive = true,
            leadingIcon = { tint -> TrashGlyph(modifier = Modifier.size(AppIconSize.md), tint = tint) },
            shortcut = when {
                !platform.isDesktop -> null
                platform.usesCommandKey -> "⌘⌫"
                else -> stringResource(Res.string.vault_delete_shortcut)
            },
        )
    }
}

/**
 * V8: the document's name and file over its actions — Save, then deleting
 * apart in `error`. Partager, Renommer, Reclasser and Informations
 * techniques wait for the SDK.
 */
@Composable
private fun DocumentActionsSheet(
    document: FydeDocument,
    onSave: () -> Unit,
    onDelete: () -> Unit,
    onMove: () -> Unit,
    onDismissRequest: () -> Unit,
) {
    AppBottomSheet(onDismissRequest = onDismissRequest) {
        Column(modifier = Modifier.padding(horizontal = AppSpacing.space5)) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(AppSpacing.space4),
            ) {
                AppIconTile { PagesGlyph(modifier = Modifier.size(AppIconSize.md)) }
                Column(modifier = Modifier.weight(1f)) {
                    Text(text = document.name, style = AppTypography.bodyEmphasis, color = AppColors.text, maxLines = 2, overflow = TextOverflow.Ellipsis)
                    Text(
                        text = stringResource(Res.string.vault_file_caption, document.typeCode, localizedSize(document.size)),
                        style = AppTypography.caption,
                        color = AppColors.textMuted,
                    )
                }
            }
            Spacer(Modifier.height(AppSpacing.space5))
            AppGroup {
                AppGroupRow(onClick = {
                    onDismissRequest()
                    onSave()
                }) {
                    DownloadGlyph(modifier = Modifier.size(AppIconSize.md), tint = AppColors.text)
                    Text(stringResource(Res.string.vault_save), style = AppTypography.body, color = AppColors.text)
                }
                AppGroupRow(onClick = {
                    onDismissRequest()
                    onMove()
                }) {
                    FolderGlyph(modifier = Modifier.size(AppIconSize.md), tint = AppColors.text)
                    Text(stringResource(Res.string.document_move), style = AppTypography.body, color = AppColors.text)
                }
            }
            Spacer(Modifier.height(AppSpacing.space4))
            AppGroup {
                AppGroupRow(onClick = {
                    onDismissRequest()
                    onDelete()
                }) {
                    TrashGlyph(modifier = Modifier.size(AppIconSize.md), tint = AppColors.error)
                    Text(stringResource(Res.string.vault_delete), style = AppTypography.body, color = AppColors.error)
                }
            }
        }
    }
}

/**
 * V10 (below Expanded) and D2c (Expanded, headed by a trash tile, the focus
 * on Annuler): erased from the vault and every device, for good. Stays up,
 * busy, while the deletion runs, and with [DocumentDeletion.error] if it
 * failed. The note that a source won't fetch it again waits for the SDK to
 * remember deleted documents and say which source brought one. For several
 * documents (V10b, D11b): how many, then the first [DELETION_NAMES_SHOWN]
 * names and "et n autres".
 */
@Composable
internal fun DeleteDocumentDialog(
    deletion: DocumentDeletion,
    deviceCount: Int?,
    wide: Boolean,
    onConfirm: () -> Unit,
    onDismiss: () -> Unit,
) {
    val documents = deletion.documents
    val single = documents.singleOrNull()
    val error = deletion.error
    AppDialog(
        onDismissRequest = { if (!deletion.busy) onDismiss() },
        title = if (single != null) {
            stringResource(Res.string.vault_delete_title)
        } else {
            pluralStringResource(Res.plurals.documents_delete_title, documents.size, documents.size)
        },
        text = when {
            single != null && deviceCount != null ->
                pluralStringResource(Res.plurals.vault_delete_text_devices, deviceCount, deviceCount, single.name)
            single != null -> stringResource(Res.string.vault_delete_text, single.name)
            deviceCount != null -> pluralStringResource(Res.plurals.documents_delete_text_devices, deviceCount, deviceCount)
            else -> stringResource(Res.string.documents_delete_text)
        },
        confirmLabel = stringResource(Res.string.vault_delete_confirm),
        onConfirm = onConfirm,
        dismissLabel = stringResource(Res.string.cancel),
        onDismiss = onDismiss,
        destructive = true,
        confirmLoading = deletion.busy,
        icon = if (wide) {
            {
                AppIconTile(size = AppDimensions.iconTileLarge, tint = AppColors.error) {
                    TrashGlyph(modifier = Modifier.size(AppIconSize.prominent), tint = AppColors.error)
                }
            }
        } else {
            null
        },
        focusDismiss = wide,
        content = if (single == null || error != null) {
            {
                Column(verticalArrangement = Arrangement.spacedBy(AppSpacing.space3)) {
                    if (single == null) DeletedNames(documents)
                    error?.let { AppErrorNote(it) }
                }
            }
        } else {
            null
        },
    )
}

/** How many names V10b lists before "et n autres". */
internal const val DELETION_NAMES_SHOWN = 3

/** V10b's list: the first [DELETION_NAMES_SHOWN] names, then how many more. */
@Composable
private fun DeletedNames(documents: List<FydeDocument>) {
    val more = documents.size - DELETION_NAMES_SHOWN
    Column(verticalArrangement = Arrangement.spacedBy(AppSpacing.space1)) {
        documents.take(DELETION_NAMES_SHOWN).forEach {
            Text(text = it.name, style = AppTypography.bodyEmphasis, color = AppColors.text, maxLines = 1, overflow = TextOverflow.Ellipsis)
        }
        if (more > 0) {
            Text(text = pluralStringResource(Res.plurals.documents_more, more, more), style = AppTypography.caption, color = AppColors.textMuted)
        }
    }
}

/** The document's file: its first page (its type's tile without one), its name, "PDF · 182 Ko", opening the viewer. */
@Composable
private fun FileRow(document: FydeDocument, size: String, onClick: () -> Unit) {
    val interactionSource = remember { MutableInteractionSource() }
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(interactionSource = interactionSource, indication = ripple(), role = Role.Button, onClick = onClick)
            .appFocusRing(interactionSource, shape = AppRadius.none)
            .semantics(mergeDescendants = true) {}
            .heightIn(min = AppDimensions.rowHeightStandard)
            .padding(horizontal = AppSpacing.space5, vertical = AppSpacing.space3),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(AppSpacing.space4),
    ) {
        val thumbnail = rememberDocumentThumbnail(document)
        if (thumbnail != null) {
            DocumentPage(thumbnail, width = AppDimensions.documentFilePageWidth, height = AppDimensions.documentFilePageHeight)
        } else {
            AppIconTile { PagesGlyph(modifier = Modifier.size(AppIconSize.md)) }
        }
        Column(modifier = Modifier.weight(1f)) {
            Text(text = document.name, style = AppTypography.bodyEmphasis, color = AppColors.text, maxLines = 1, overflow = TextOverflow.Ellipsis)
            Text(
                text = stringResource(Res.string.vault_file_caption, document.typeCode, size),
                style = AppTypography.caption,
                color = AppColors.textMuted,
                maxLines = 1,
            )
        }
        ChevronRightGlyph(modifier = Modifier.size(AppIconSize.sm))
    }
}

@Composable
private fun FactRow(label: String, value: String) {
    AppGroupRow(minHeight = AppDimensions.rowHeightCompact) {
        Text(text = label, style = AppTypography.body, color = AppColors.textMuted, modifier = Modifier.weight(1f))
        Text(text = value, style = AppTypography.body, color = AppColors.text, textAlign = TextAlign.End)
    }
}
