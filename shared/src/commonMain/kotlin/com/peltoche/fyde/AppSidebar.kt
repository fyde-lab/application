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
import androidx.compose.ui.unit.sp
import com.peltoche.fyde.components.content.AppDivider
import com.peltoche.fyde.resources.Res
import com.peltoche.fyde.resources.app_title
import com.peltoche.fyde.resources.nav_scripts
import com.peltoche.fyde.resources.settings
import com.peltoche.fyde.resources.sidebar_all_vault
import com.peltoche.fyde.resources.sidebar_categories
import com.peltoche.fyde.resources.sidebar_item_count
import com.peltoche.fyde.resources.vault_sealed
import com.peltoche.fyde.resources.vault_sealed_devices
import com.peltoche.fyde.theme.AppColors
import com.peltoche.fyde.theme.AppIconSize
import com.peltoche.fyde.theme.AppRadius
import com.peltoche.fyde.theme.AppSpacing
import com.peltoche.fyde.theme.AppTypography
import com.peltoche.fyde.theme.appFocusRing
import org.jetbrains.compose.resources.pluralStringResource
import org.jetbrains.compose.resources.stringResource

/** What the sidebar highlights: a destination, or one of the vault's categories. */
sealed interface SidebarSelection {
    data object Vault : SidebarSelection
    data class Category(val category: FydeCategory) : SidebarSelection
    data object Sources : SidebarSelection
    data object Settings : SidebarSelection
}

/**
 * The desktop sidebar (design-system.md § Layout, D1–D6 and T1): the brand,
 * "Tout le coffre", the categories holding documents, then Sources, the seal
 * line and Settings at its foot. 260dp wide. Arrivées is left out until
 * there's an inbox to open.
 */
@Composable
internal fun AppSidebar(
    navigation: TopLevelNavigation,
    selection: SidebarSelection,
    modifier: Modifier = Modifier,
) {
    val summary = navigation.summary
    Row(modifier = modifier.width(SidebarWidth).fillMaxHeight()) {
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
                selectedFill = AppColors.selectionSurface,
                onClick = navigation.onOpenVault,
            ) { tint -> GridGlyph(tint = tint, modifier = Modifier.size(AppIconSize.md)) }
            Column(
                modifier = Modifier.weight(1f).verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(AppSpacing.space1),
            ) {
                if (summary.categories.isNotEmpty()) {
                    Text(
                        text = stringResource(Res.string.sidebar_categories).uppercase(),
                        style = AppTypography.micro.copy(fontWeight = FontWeight.SemiBold, letterSpacing = 0.72.sp),
                        color = AppColors.textMuted,
                        modifier = Modifier.padding(start = 10.dp, end = 10.dp, top = AppSpacing.space5, bottom = 6.dp),
                    )
                }
                summary.categories.forEach { (category, count) ->
                    val colors = category.colors()
                    SidebarRow(
                        label = category.label(),
                        count = count,
                        selected = selection == SidebarSelection.Category(category),
                        selectedFill = colors.fill,
                        onClick = { navigation.onOpenCategory(category) },
                    ) {
                        CategoryTile(category, size = 24.dp, iconSize = AppIconSize.sm, shape = RoundedCornerShape(7.dp))
                    }
                }
            }
            SidebarRow(
                label = stringResource(Res.string.nav_scripts),
                count = summary.sourceCount?.toLong(),
                selected = selection == SidebarSelection.Sources,
                selectedFill = AppColors.selectionSurface,
                onClick = navigation.onOpenScripts,
            ) { tint -> PlugGlyph(tint = tint, modifier = Modifier.size(AppIconSize.md)) }
            SealRow(summary.deviceCount)
            SidebarRow(
                label = stringResource(Res.string.settings),
                count = null,
                selected = selection == SidebarSelection.Settings,
                selectedFill = AppColors.selectionSurface,
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
    Row(modifier = modifier.width(SidebarRailWidth).fillMaxHeight()) {
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
            ) { tint -> GridGlyph(tint = tint, modifier = Modifier.size(24.dp)) }
            Box(Modifier.padding(vertical = AppSpacing.space2).width(32.dp).height(1.dp).background(AppColors.border))
            Column(
                modifier = Modifier.weight(1f).verticalScroll(rememberScrollState()),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(AppSpacing.space1),
            ) {
                summary.categories.forEach { (category, count) ->
                    RailCell(
                        label = category.label(),
                        count = count,
                        selected = selection == SidebarSelection.Category(category),
                        onClick = { navigation.onOpenCategory(category) },
                    ) {
                        CategoryTile(category, size = AppIconSize.xl, iconSize = AppIconSize.md, shape = RailCellShape)
                    }
                }
            }
            RailCell(
                label = stringResource(Res.string.nav_scripts),
                count = summary.sourceCount?.toLong(),
                selected = selection == SidebarSelection.Sources,
                onClick = navigation.onOpenScripts,
            ) { tint -> PlugGlyph(tint = tint, modifier = Modifier.size(24.dp)) }
            RailCell(
                label = stringResource(Res.string.settings),
                count = null,
                selected = selection == SidebarSelection.Settings,
                onClick = navigation.onOpenSettings,
            ) { tint -> GearGlyph(tint = tint, modifier = Modifier.size(24.dp)) }
        }
        AppDivider(vertical = true)
    }
}

/** One sidebar row: selected, it takes [selectedFill] (its category's for a category) and a primary leading bar. */
@Composable
private fun SidebarRow(
    label: String,
    count: Long?,
    selected: Boolean,
    selectedFill: Color,
    onClick: () -> Unit,
    leading: @Composable (tint: Color) -> Unit,
) {
    val interactionSource = remember { MutableInteractionSource() }
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .height(SidebarRowHeight)
            .appFocusRing(interactionSource, shape = AppRadius.lg)
            .clip(AppRadius.lg)
            .selectionMarker(selected, selectedFill)
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
        modifier = Modifier.fillMaxWidth().height(SidebarRowHeight).padding(horizontal = 10.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        ShieldGlyph(modifier = Modifier.size(AppIconSize.md))
        Text(
            text = if (deviceCount != null) {
                pluralStringResource(Res.plurals.vault_sealed_devices, deviceCount, deviceCount)
            } else {
                stringResource(Res.string.vault_sealed)
            },
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
                .size(RailCellSize)
                .appFocusRing(interactionSource, shape = RailCellShape)
                .clip(RailCellShape)
                .selectionMarker(selected, AppColors.selectionSurface)
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

@Composable
private fun CategoryTile(category: FydeCategory, size: Dp, iconSize: Dp, shape: Shape) {
    val colors = category.colors()
    Box(
        modifier = Modifier.size(size).clip(shape).background(colors.fill),
        contentAlignment = Alignment.Center,
    ) {
        CategoryGlyph(category, tint = colors.ink, modifier = Modifier.size(iconSize))
    }
}

/** The selected item's fill and its 3dp primary bar along the leading edge, inside the item's clip. */
@Composable
private fun Modifier.selectionMarker(selected: Boolean, fill: Color): Modifier {
    if (!selected) return this
    val bar = AppColors.primary
    return background(fill).drawBehind {
        val width = SelectionBarWidth.toPx()
        val x = if (layoutDirection == LayoutDirection.Rtl) size.width - width else 0f
        drawRect(bar, topLeft = Offset(x, 0f), size = Size(width, size.height))
    }
}

/** Both widths include the trailing divider. */
internal val SidebarWidth = 260.dp
internal val SidebarRailWidth = 80.dp

/** The sidebar takes the window's leading edge, so it keeps clear of the system bars and cutouts there; the content handles the rest. */
internal val SidebarInsets: WindowInsets
    @Composable get() = WindowInsets.safeDrawing.only(WindowInsetsSides.Vertical + WindowInsetsSides.Start)
private val SidebarRowHeight = 40.dp
private val RailCellSize = 48.dp
private val RailCellShape = RoundedCornerShape(12.dp)
private val SelectionBarWidth = 3.dp
