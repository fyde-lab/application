package com.peltoche.fyde

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.peltoche.fyde.components.navigation.AppNavigationItem
import com.peltoche.fyde.resources.Res
import com.peltoche.fyde.resources.encryption_badge_description
import com.peltoche.fyde.resources.encryption_badge_title
import com.peltoche.fyde.resources.nav_addons
import com.peltoche.fyde.resources.nav_search
import com.peltoche.fyde.resources.scripts_menu_item
import com.peltoche.fyde.resources.settings
import com.peltoche.fyde.resources.sidebar_app_title
import com.peltoche.fyde.theme.AppColors
import com.peltoche.fyde.theme.AppRadius
import com.peltoche.fyde.theme.AppSpacing
import com.peltoche.fyde.theme.AppTypography
import org.jetbrains.compose.resources.stringResource

/**
 * The persistent Expanded-breakpoint sidebar's real destinations — shared by
 * every screen that shows it ([DocumentListScreen], [ScriptListScreen],
 * [AddonsScreen], [SettingsScreen] via [SidebarFooter]) so navigating
 * between them is consistent rather than each screen building its own list.
 * Used to also carry several `enabled = false` placeholders (All documents,
 * Categories, Suppliers, Issuers, Dates, Favorites) that never linked
 * anywhere real — dropped rather than shipped as dead links.
 */
enum class SidebarDestination { Documents, Scripts, Addons, Settings }

@Composable
fun sidebarNavigationItems(
    current: SidebarDestination,
    onOpenDocuments: () -> Unit,
    onOpenScripts: () -> Unit,
    onOpenAddons: () -> Unit,
): List<AppNavigationItem> = listOf(
    AppNavigationItem(
        label = stringResource(Res.string.nav_search),
        selected = current == SidebarDestination.Documents,
        onClick = onOpenDocuments,
        icon = {
            SearchGlyph(
                tint = if (current == SidebarDestination.Documents) AppColors.primary else AppColors.textMuted,
                modifier = Modifier.size(20.dp),
            )
        },
    ),
    AppNavigationItem(
        label = stringResource(Res.string.scripts_menu_item),
        selected = current == SidebarDestination.Scripts,
        onClick = onOpenScripts,
        icon = {
            CodeGlyph(
                tint = if (current == SidebarDestination.Scripts) AppColors.primary else AppColors.textMuted,
                modifier = Modifier.size(20.dp),
            )
        },
    ),
    AppNavigationItem(
        label = stringResource(Res.string.nav_addons),
        selected = current == SidebarDestination.Addons,
        onClick = onOpenAddons,
        icon = {
            PuzzleGlyph(
                tint = if (current == SidebarDestination.Addons) AppColors.primary else AppColors.textMuted,
                modifier = Modifier.size(20.dp),
            )
        },
    ),
)

@Composable
fun SidebarHeader() {
    Row(
        modifier = Modifier.fillMaxWidth().padding(horizontal = AppSpacing.space5, vertical = AppSpacing.space6),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        DocumentGlyph(modifier = Modifier.size(22.dp), tint = AppColors.primary, strokeWidth = 1.8.dp)
        Spacer(Modifier.width(AppSpacing.space3))
        Text(
            text = stringResource(Res.string.sidebar_app_title),
            style = AppTypography.section,
            color = AppColors.text,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
        )
    }
}

/** Sidebar foot: a static end-to-end-encryption trust badge (a real property of this app, see the top-level design docs), then the real Settings destination. */
@Composable
fun SidebarFooter(current: SidebarDestination, onOpenSettings: () -> Unit) {
    val settingsSelected = current == SidebarDestination.Settings
    Column(modifier = Modifier.padding(AppSpacing.space3)) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clip(AppRadius.md)
                .background(AppColors.success.copy(alpha = 0.10f))
                .padding(AppSpacing.space3),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            LockGlyph(modifier = Modifier.size(18.dp))
            Spacer(Modifier.width(AppSpacing.space3))
            Column {
                Text(stringResource(Res.string.encryption_badge_title), style = AppTypography.caption, color = AppColors.text)
                Text(stringResource(Res.string.encryption_badge_description), style = AppTypography.micro, color = AppColors.textMuted)
            }
        }
        Spacer(Modifier.height(AppSpacing.space2))
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clip(AppRadius.md)
                .background(if (settingsSelected) AppColors.primary.copy(alpha = 0.10f) else Color.Transparent)
                .clickable(onClick = onOpenSettings)
                .padding(horizontal = AppSpacing.space3, vertical = AppSpacing.space4),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            GearGlyph(tint = if (settingsSelected) AppColors.primary else AppColors.textMuted, modifier = Modifier.size(20.dp))
            Spacer(Modifier.width(AppSpacing.space4))
            Text(
                stringResource(Res.string.settings),
                style = AppTypography.label,
                color = if (settingsSelected) AppColors.text else AppColors.textMuted,
            )
        }
    }
}
