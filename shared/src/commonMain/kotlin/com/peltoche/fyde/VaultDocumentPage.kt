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
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import com.peltoche.fyde.components.action.AppButton
import com.peltoche.fyde.components.action.AppCircleButton
import com.peltoche.fyde.components.content.AppDivider
import com.peltoche.fyde.components.content.AppIconTile
import com.peltoche.fyde.components.content.DocumentAttributes
import com.peltoche.fyde.components.content.DocumentPass
import com.peltoche.fyde.resources.Res
import com.peltoche.fyde.resources.vault_fact_arrived
import com.peltoche.fyde.resources.vault_fact_encrypted
import com.peltoche.fyde.resources.vault_fact_encrypted_devices
import com.peltoche.fyde.resources.vault_fact_filed
import com.peltoche.fyde.resources.vault_info_heading
import com.peltoche.fyde.resources.vault_fact_in_vault
import com.peltoche.fyde.resources.vault_file_caption
import com.peltoche.fyde.resources.vault_filed_path
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

/** V6: a document's pass, its file (opening the viewer), its facts, and Save. */
@Composable
internal fun DocumentPage(vault: Vault, document: FydeDocument) {
    Column(modifier = Modifier.fillMaxSize().verticalScroll(rememberScrollState())) {
        VaultPageBar(title = document.pathTitle(), onBack = vault::closeDocument)
        VaultNotes(vault)
        Spacer(Modifier.height(AppSpacing.space3))
        DocumentDetails(
            vault = vault,
            document = document,
            onOpenFile = { vault.onFocusChange(vault.focus.copy(viewerOpen = true)) },
        )
        Spacer(Modifier.height(AppSpacing.space7))
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
                VaultPageBar(title = document.pathTitle(), onBack = onBack)
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
    val information = remember(document, layout) { informationRows(document, layout) }
    val pass = passText(document, layout, fallbackIssuer = document.categoryLabel())
    val type = document.documentType?.label()
    Column {
        DocumentPass(
            issuer = pass.issuer,
            type = type,
            headline = pass.headline,
            line = pass.line,
            cells = pass.cells,
            color = document.category.colors(),
            headlineKind = pass.headlineKind,
            description = listOfNotNull(listOfNotNull(type, pass.issuer).joinToString(" "), pass.headline, pass.line)
                .plus(pass.cells.map { "${it.label} ${it.value}" })
                .filter { it.isNotBlank() }
                .joinToString(", "),
            icon = { tint -> CategoryGlyph(document.category, tint) },
        )
        if (onOpenFile != null) {
            Spacer(Modifier.height(AppSpacing.space5))
            SettingsGroup { FileRow(document, size, onOpenFile) }
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
        SettingsGroup {
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
            AppDivider()
            FactRow(stringResource(Res.string.vault_fact_filed), document.filedPath())
        }
        Spacer(Modifier.height(AppSpacing.space5))
        AppButton(
            text = stringResource(Res.string.vault_save),
            onClick = { vault.onSave(document) },
            modifier = Modifier.fillMaxWidth(),
            leadingIcon = { DownloadGlyph(modifier = Modifier.size(AppIconSize.sm), tint = AppColors.onPrimary) },
        )
    }
}

/** The document's file: its name, "PDF · 182 Ko", opening the viewer. */
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
        AppIconTile { PagesGlyph(modifier = Modifier.size(AppIconSize.md)) }
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
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .heightIn(min = AppDimensions.rowHeightCompact)
            .padding(horizontal = AppSpacing.space5, vertical = AppSpacing.space3),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(AppSpacing.space4),
    ) {
        Text(text = label, style = AppTypography.body, color = AppColors.textMuted, modifier = Modifier.weight(1f))
        Text(text = value, style = AppTypography.body, color = AppColors.text, textAlign = TextAlign.End)
    }
}

/** "Logement · Énergie": where the document is filed, for its page's bar. */
@Composable
private fun FydeDocument.pathTitle(): String =
    listOfNotNull(categoryLabel(), subcategory?.label()).joinToString(" · ")

/** "Logement › Énergie", the category alone, or "À classer". */
@Composable
private fun FydeDocument.filedPath(): String {
    val subcategory = subcategory
    return if (category != null && subcategory != null) {
        stringResource(Res.string.vault_filed_path, categoryLabel(), subcategory.label())
    } else {
        categoryLabel()
    }
}
