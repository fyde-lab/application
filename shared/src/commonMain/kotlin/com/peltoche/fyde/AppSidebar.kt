package com.peltoche.fyde

import androidx.compose.foundation.background
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.WindowInsetsSides
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.only
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.PlainTooltip
import androidx.compose.material3.Text
import androidx.compose.material3.TooltipAnchorPosition
import androidx.compose.material3.TooltipBox
import androidx.compose.material3.TooltipDefaults
import androidx.compose.material3.rememberTooltipState
import androidx.compose.material3.ripple
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.selected
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import com.peltoche.fyde.components.action.AppIconButton
import com.peltoche.fyde.components.content.AppDivider
import com.peltoche.fyde.components.content.FolderFillTile
import com.peltoche.fyde.icons.GearGlyph
import com.peltoche.fyde.icons.GridGlyph
import com.peltoche.fyde.icons.PlugGlyph
import com.peltoche.fyde.icons.PlusGlyph
import com.peltoche.fyde.icons.ShieldGlyph
import com.peltoche.fyde.resources.Res
import com.peltoche.fyde.resources.app_title
import com.peltoche.fyde.resources.folder_new
import com.peltoche.fyde.resources.nav_scripts
import com.peltoche.fyde.resources.settings
import com.peltoche.fyde.resources.sidebar_all_vault
import com.peltoche.fyde.resources.sidebar_folders
import com.peltoche.fyde.resources.sidebar_item_count
import com.peltoche.fyde.resources.vault_unclassified
import com.peltoche.fyde.theme.AppColors
import com.peltoche.fyde.theme.AppDimensions
import com.peltoche.fyde.theme.AppIconSize
import com.peltoche.fyde.theme.AppRadius
import com.peltoche.fyde.theme.AppShapes
import com.peltoche.fyde.theme.AppSpacing
import com.peltoche.fyde.theme.AppTypography
import com.peltoche.fyde.theme.appFocusRing
import org.jetbrains.compose.resources.stringResource

/** What the sidebar highlights: a destination, or one of the vault's folders (`null`: À classer). */
sealed interface SidebarSelection {
    data object Vault : SidebarSelection
    data class Folder(val folderId: String?) : SidebarSelection
    data object Sources : SidebarSelection
    data object Settings : SidebarSelection
}

/**
 * The desktop sidebar (design-system.md § Layout, D1–D6 and T1): the brand,
 * "Tout le coffre", À classer while it holds documents, DOSSIERS with its
 * "+" (a new top-level folder, D10), the folder tree
 * (top-level folders by name, the open folder's branch expanded), then
 * Sources, the seal line and Settings at its foot. 260dp wide.
 */
@Composable
internal fun AppSidebar(
    navigation: TopLevelNavigation,
    selection: SidebarSelection,
    modifier: Modifier = Modifier,
) {
    val summary = navigation.summary
    Row(modifier = modifier.width(AppDimensions.sidebarWidth).fillMaxHeight()) {
        Column(
            modifier = Modifier
                .weight(1f)
                .fillMaxHeight()
                .background(AppColors.surface)
                .windowInsetsPadding(SidebarInsets)
                .padding(horizontal = 14.dp, vertical = AppSpacing.space6),
            verticalArrangement = Arrangement.spacedBy(AppSpacing.space1),
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.padding(start = 10.dp, end = 10.dp, bottom = AppSpacing.space5),
            ) {
                FydeLogo(modifier = Modifier.size(30.dp))
                Spacer(Modifier.width(10.dp))
                Text(
                    text = stringResource(Res.string.app_title),
                    style = AppTypography.section.copy(fontWeight = FontWeight.SemiBold),
                    color = AppColors.text,
                )
            }
            SidebarRow(
                label = stringResource(Res.string.sidebar_all_vault),
                count = summary.total,
                selected = selection == SidebarSelection.Vault,
                onClick = navigation.onOpenVault,
            ) { tint -> GridGlyph(tint = tint, modifier = Modifier.size(AppIconSize.md)) }
            Column(
                modifier = Modifier.weight(1f).verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(AppSpacing.space1),
            ) {
                if (summary.unfiled > 0) {
                    SidebarRow(
                        label = stringResource(Res.string.vault_unclassified),
                        count = summary.unfiled,
                        selected = selection == SidebarSelection.Folder(null),
                        onClick = { navigation.onOpenFolder(null) },
                        modifier = Modifier.vaultDropTarget(FolderChoice(null)),
                    ) {
                        UnfiledTile(size = 24.dp, iconSize = AppIconSize.sm, shape = RoundedCornerShape(7.dp))
                    }
                }
                // Shown even without folders, so the first one can be made from here.
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.padding(start = 10.dp, top = AppSpacing.space4, bottom = 2.dp),
                ) {
                    Text(
                        text = stringResource(Res.string.sidebar_folders).uppercase(),
                        style = AppTypography.overline,
                        color = AppColors.textMuted,
                        modifier = Modifier.weight(1f),
                    )
                    AppIconButton(onClick = navigation.onNewFolder, contentDescription = stringResource(Res.string.folder_new)) {
                        PlusGlyph(modifier = Modifier.size(AppIconSize.sm), tint = AppColors.textMuted)
                    }
                }
                val openBranch = (selection as? SidebarSelection.Folder)?.folderId
                    ?.let { open -> summary.folders.path(open).map { it.id }.toSet() }
                    .orEmpty()
                SidebarFolderRows(navigation, selection, summary.folders.topLevel, openBranch, depth = 0)
            }
            SidebarRow(
                label = stringResource(Res.string.nav_scripts),
                count = summary.sourceCount?.toLong(),
                selected = selection == SidebarSelection.Sources,
                onClick = navigation.onOpenScripts,
            ) { tint -> PlugGlyph(tint = tint, modifier = Modifier.size(AppIconSize.md)) }
            SealRow(summary.deviceCount)
            SidebarRow(
                label = stringResource(Res.string.settings),
                count = null,
                selected = selection == SidebarSelection.Settings,
                onClick = navigation.onOpenSettings,
            ) { tint -> GearGlyph(tint = tint, modifier = Modifier.size(AppIconSize.md)) }
        }
        AppDivider(vertical = true)
    }
}

/**
 * The sidebar at Medium (T2): icons only, 80dp wide. Each category is its
 * tile; a cell's name and count are in its tooltip and accessibility label.
 */
@Composable
internal fun AppSidebarRail(
    navigation: TopLevelNavigation,
    selection: SidebarSelection,
    modifier: Modifier = Modifier,
) {
    val summary = navigation.summary
    Row(modifier = modifier.width(AppDimensions.sidebarRailWidth).fillMaxHeight()) {
        Column(
            modifier = Modifier
                .weight(1f)
                .fillMaxHeight()
                .background(AppColors.surface)
                .windowInsetsPadding(SidebarInsets)
                .padding(vertical = 18.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            FydeLogo(
                modifier = Modifier.padding(bottom = 10.dp).size(AppIconSize.xl),
                contentDescription = stringResource(Res.string.app_title),
            )
            RailCell(
                label = stringResource(Res.string.sidebar_all_vault),
                count = summary.total,
                selected = selection == SidebarSelection.Vault,
                onClick = navigation.onOpenVault,
            ) { tint -> GridGlyph(tint = tint, modifier = Modifier.size(AppIconSize.prominent)) }
            Box(Modifier.padding(vertical = AppSpacing.space2).width(32.dp).height(1.dp).background(AppColors.border))
            Column(
                modifier = Modifier.weight(1f).verticalScroll(rememberScrollState()),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(AppSpacing.space1),
            ) {
                if (summary.unfiled > 0) {
                    RailCell(
                        label = stringResource(Res.string.vault_unclassified),
                        count = summary.unfiled,
                        selected = selection == SidebarSelection.Folder(null),
                        onClick = { navigation.onOpenFolder(null) },
                    ) {
                        UnfiledTile(size = AppIconSize.xl, iconSize = AppIconSize.md, shape = AppShapes.sidebarRailCell)
                    }
                }
                summary.folders.topLevel.forEach { folder ->
                    // A subfolder's page keeps its top-level folder marked.
                    val open = (selection as? SidebarSelection.Folder)?.folderId
                    RailCell(
                        label = folder.name,
                        count = summary.folderCounts[folder.id] ?: 0,
                        selected = open != null && summary.folders.topLevelOf(open)?.id == folder.id,
                        onClick = { navigation.onOpenFolder(folder.id) },
                    ) {
                        FolderTile(summary.folders, folder, size = AppIconSize.xl, iconSize = AppIconSize.md, shape = AppShapes.sidebarRailCell)
                    }
                }
            }
            RailCell(
                label = stringResource(Res.string.nav_scripts),
                count = summary.sourceCount?.toLong(),
                selected = selection == SidebarSelection.Sources,
                onClick = navigation.onOpenScripts,
            ) { tint -> PlugGlyph(tint = tint, modifier = Modifier.size(AppIconSize.prominent)) }
            RailCell(
                label = stringResource(Res.string.settings),
                count = null,
                selected = selection == SidebarSelection.Settings,
                onClick = navigation.onOpenSettings,
            ) { tint -> GearGlyph(tint = tint, modifier = Modifier.size(AppIconSize.prominent)) }
        }
        AppDivider(vertical = true)
    }
}

/** One sidebar row: selected, it takes the selection fill and a primary leading bar. */
@Composable
private fun SidebarRow(
    label: String,
    count: Long?,
    selected: Boolean,
    onClick: () -> Unit,
    indent: Dp = 0.dp,
    modifier: Modifier = Modifier,
    leading: @Composable (tint: Color) -> Unit,
) {
    val interactionSource = remember { MutableInteractionSource() }
    Row(
        modifier = modifier
            .padding(start = indent)
            .fillMaxWidth()
            .height(AppDimensions.sidebarRowHeight)
            .appFocusRing(interactionSource, shape = AppRadius.lg)
            .clip(AppRadius.lg)
            .selectionMarker(selected)
            .selectable(
                selected = selected,
                interactionSource = interactionSource,
                indication = ripple(),
                role = Role.Tab,
                onClick = onClick,
            )
            .padding(horizontal = 10.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        leading(AppColors.text)
        Text(
            text = label,
            style = AppTypography.body.copy(fontWeight = if (selected) FontWeight.SemiBold else FontWeight.Medium),
            color = AppColors.text,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier.weight(1f),
        )
        if (count != null) {
            Text(text = count.toString(), style = AppTypography.caption, color = AppColors.textMuted)
        }
    }
}

/** "Scellé · 3 appareils": a statement, not a destination, so not clickable. */
@Composable
private fun SealRow(deviceCount: Int?) {
    Row(
        modifier = Modifier.fillMaxWidth().height(AppDimensions.sidebarRowHeight).padding(horizontal = 10.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        ShieldGlyph(modifier = Modifier.size(AppIconSize.md))
        Text(
            text = sealedText(deviceCount),
            style = AppTypography.body.copy(fontWeight = FontWeight.Medium),
            color = AppColors.success,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun RailCell(
    label: String,
    count: Long?,
    selected: Boolean,
    onClick: () -> Unit,
    icon: @Composable (tint: Color) -> Unit,
) {
    val description = if (count != null) stringResource(Res.string.sidebar_item_count, label, count.toString()) else label
    val interactionSource = remember { MutableInteractionSource() }
    TooltipBox(
        positionProvider = TooltipDefaults.rememberTooltipPositionProvider(TooltipAnchorPosition.End),
        tooltip = { PlainTooltip { Text(description) } },
        state = rememberTooltipState(),
    ) {
        Box(
            modifier = Modifier
                .size(AppDimensions.sidebarRailCell)
                .appFocusRing(interactionSource, shape = AppShapes.sidebarRailCell)
                .clip(AppShapes.sidebarRailCell)
                .selectionMarker(selected)
                .selectable(
                    selected = selected,
                    interactionSource = interactionSource,
                    indication = ripple(),
                    role = Role.Tab,
                    onClick = onClick,
                )
                .clearAndSetSemantics {
                    contentDescription = description
                    role = Role.Tab
                    this.selected = selected
                },
            contentAlignment = Alignment.Center,
        ) {
            icon(if (selected) AppColors.primary else AppColors.text)
        }
    }
}

/**
 * [folders]' rows at [depth], each with its count (its subfolders'
 * included); a folder on the [openBranch] (the open folder and the folders
 * above it) shows its subfolders under it, indented.
 */
@Composable
private fun SidebarFolderRows(
    navigation: TopLevelNavigation,
    selection: SidebarSelection,
    folders: List<FydeFolder>,
    openBranch: Set<String>,
    depth: Int,
) {
    val summary = navigation.summary
    folders.forEach { folder ->
        SidebarRow(
            label = folder.name,
            count = summary.folderCounts[folder.id] ?: 0,
            selected = selection == SidebarSelection.Folder(folder.id),
            onClick = { navigation.onOpenFolder(folder.id) },
            indent = SidebarIndent * depth,
            modifier = Modifier.vaultDropTarget(FolderChoice(folder.id)),
        ) {
            FolderTile(summary.folders, folder, size = 24.dp, iconSize = AppIconSize.sm, shape = RoundedCornerShape(7.dp))
        }
        if (folder.id in openBranch) {
            SidebarFolderRows(navigation, selection, summary.folders.children(folder.id), openBranch, depth + 1)
        }
    }
}

private val SidebarIndent = 16.dp

/** [folder]'s mark on its top-level folder's fill. */
@Composable
private fun FolderTile(folders: VaultFolders, folder: FydeFolder, size: Dp, iconSize: Dp, shape: Shape) {
    FolderFillTile(size = size, color = folders.appearanceOf(folder.id).colors(), shape = shape, iconSize = iconSize) { tint ->
        FolderMark(folder, tint = tint)
    }
}

/** À classer's document on its own fill. */
@Composable
private fun UnfiledTile(size: Dp, iconSize: Dp, shape: Shape) {
    FolderFillTile(size = size, color = null.colors(), shape = shape, iconSize = iconSize) { tint ->
        FolderIconGlyph(null, tint = tint)
    }
}

/** The selected item's fill and its 3dp primary bar along the leading edge, inside the item's clip. */
@Composable
private fun Modifier.selectionMarker(selected: Boolean): Modifier {
    if (!selected) return this
    val fill = AppColors.selectionSurface
    val bar = AppColors.primary
    return background(fill).drawBehind {
        val width = AppDimensions.selectionBarWidth.toPx()
        val x = if (layoutDirection == LayoutDirection.Rtl) size.width - width else 0f
        drawRect(bar, topLeft = Offset(x, 0f), size = Size(width, size.height))
    }
}

/** Both widths include the trailing divider. */

/** The sidebar takes the window's leading edge, so it keeps clear of the system bars and cutouts there; the content handles the rest. */
internal val SidebarInsets: WindowInsets
    @Composable get() = WindowInsets.safeDrawing.only(WindowInsetsSides.Vertical + WindowInsetsSides.Start)
