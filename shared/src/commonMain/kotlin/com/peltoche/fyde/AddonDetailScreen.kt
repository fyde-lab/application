package com.peltoche.fyde

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.peltoche.fyde.components.action.AppButton
import com.peltoche.fyde.components.action.AppButtonVariant
import com.peltoche.fyde.components.content.AppBadge
import com.peltoche.fyde.components.content.AppBadgeVariant
import com.peltoche.fyde.components.content.AppDivider
import com.peltoche.fyde.components.feedback.AppBanner
import com.peltoche.fyde.components.navigation.AppTopBar
import com.peltoche.fyde.layout.AppContentContainer
import com.peltoche.fyde.layout.AppContentWidth
import com.peltoche.fyde.layout.AppWindowSizeClass
import com.peltoche.fyde.layout.LocalAppWindowSizeClass
import com.peltoche.fyde.resources.Res
import com.peltoche.fyde.resources.addon_about_section
import com.peltoche.fyde.resources.addon_detail_sites
import com.peltoche.fyde.resources.addon_detail_updated
import com.peltoche.fyde.resources.addon_detail_version
import com.peltoche.fyde.resources.addon_details_section
import com.peltoche.fyde.resources.addon_domains_installed
import com.peltoche.fyde.resources.addon_domains_not_installed
import com.peltoche.fyde.resources.addon_install_button
import com.peltoche.fyde.resources.addon_installed_badge
import com.peltoche.fyde.resources.addon_no_domains
import com.peltoche.fyde.resources.addon_site_access_section
import com.peltoche.fyde.resources.addon_uninstall_button
import com.peltoche.fyde.resources.addons_title
import com.peltoche.fyde.resources.back
import com.peltoche.fyde.resources.dismiss
import com.peltoche.fyde.resources.script_version
import com.peltoche.fyde.theme.AppColors
import com.peltoche.fyde.theme.AppSpacing
import com.peltoche.fyde.theme.AppTypography
import org.jetbrains.compose.resources.stringResource

/** Keeps the install/uninstall button a deliberate target, not a word-sized one, when it isn't full width. */
private val ActionButtonMinWidth = 140.dp

/**
 * Below this content width the header can't hold the logo, the name, and
 * the action button side by side, and three detail cells can't fit a
 * date — so the action drops below the header (full width) and the grid
 * goes to two columns. Measured, not tied to [AppWindowSizeClass]: the
 * Medium detail pane beside a fixed 360dp list is narrower than a Compact
 * phone screen.
 */
private val NarrowContentWidth = 480.dp

/**
 * Compact-only drill-down for one add-on, opened by tapping a row in
 * [AddonsScreen] — at Medium/Expanded the same [AddonDetailContent] renders
 * in [AddonsScreen]'s own detail pane instead, so this screen only exists
 * where there's no room for the list beside it.
 *
 * Composed in the same [TopLevelScaffold] as [AddonsScreen], with Add-ons
 * still selected, so navigation stays put; the top bar names the parent
 * ("Add-ons") with a back chevron, and the add-on's own name appears once,
 * in the header. Scrolls as one column (banner included) so a long
 * description can never push the install action out of reach, and renders
 * [banner] so an install/uninstall failure triggered from here isn't silent.
 */
@Composable
fun AddonDetailScreen(
    script: FydeScript,
    isInstalled: Boolean,
    isPending: Boolean,
    onBack: () -> Unit,
    onInstall: () -> Unit,
    onUninstall: () -> Unit,
    modifier: Modifier = Modifier,
    banner: String? = null,
    onDismissBanner: () -> Unit = {},
    onOpenDocuments: () -> Unit = {},
    onOpenScripts: () -> Unit = {},
    onOpenSettings: () -> Unit = {},
) {
    val windowSizeClass = LocalAppWindowSizeClass.current

    TopLevelScaffold(
        current = SidebarDestination.Addons,
        onOpenDocuments = onOpenDocuments,
        onOpenScripts = onOpenScripts,
        // Re-selecting Add-ons from its own drill-down returns to the list.
        onOpenAddons = onBack,
        onOpenSettings = onOpenSettings,
        modifier = modifier,
        topBar = {
            AppTopBar(
                title = stringResource(Res.string.addons_title),
                onBack = onBack,
                backContentDescription = stringResource(Res.string.back),
                windowSizeClass = windowSizeClass,
            )
        },
    ) { contentPadding ->
        AppContentContainer(
            modifier = Modifier
                .fillMaxSize()
                .padding(contentPadding)
                .background(AppColors.background),
            width = AppContentWidth.Detail,
            windowSizeClass = windowSizeClass,
            sidebarAdjacent = windowSizeClass == AppWindowSizeClass.Expanded,
        ) {
            Column(modifier = Modifier.fillMaxSize().verticalScroll(rememberScrollState())) {
                banner?.let { message ->
                    AppBanner(message = message, onDismiss = onDismissBanner, dismissLabel = stringResource(Res.string.dismiss))
                }
                // The container already supplies the screen margin.
                AddonDetailContent(
                    script = script,
                    isInstalled = isInstalled,
                    isPending = isPending,
                    onInstall = onInstall,
                    onUninstall = onUninstall,
                    horizontalPadding = 0.dp,
                )
            }
        }
    }
}

/**
 * One add-on's full record, laid out per design-system.md § Detail panes:
 * full-bleed sections separated by hairline dividers, in the order a user
 * deciding whether to install needs them —
 *
 * 1. **Header** — logo, name (+ Installed badge), tagline, and the
 *    install/uninstall action on the same row (below it, full width, when
 *    narrower than [NarrowContentWidth]).
 * 2. **Details** — version, last update, and site count as a label/value
 *    grid ([DetailCell], shared with the document detail pane).
 * 3. **Site access** — the domains the script may reach, a permission
 *    disclosure, so it comes before the description rather than trailing
 *    it, and says outright when there are none.
 * 4. **About** — the full description, omitted when there isn't one.
 *
 * Scroll-agnostic: the caller (pane or drill-down) owns scrolling, and
 * [horizontalPadding] — the pane has no margin of its own, the drill-down's
 * [AppContentContainer] already does.
 */
@Composable
internal fun AddonDetailContent(
    script: FydeScript,
    isInstalled: Boolean,
    isPending: Boolean,
    onInstall: () -> Unit,
    onUninstall: () -> Unit,
    horizontalPadding: Dp,
    modifier: Modifier = Modifier,
) {
    val sectionModifier = Modifier.fillMaxWidth().padding(horizontal = horizontalPadding, vertical = AppSpacing.space5)

    BoxWithConstraints(modifier = modifier.fillMaxWidth()) {
        val isNarrow = maxWidth < NarrowContentWidth
        Column(modifier = Modifier.fillMaxWidth()) {
            Column(modifier = sectionModifier) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    AddonIcon(icon = script.icon, size = 56.dp)
                    Spacer(Modifier.width(AppSpacing.space4))
                    Column(modifier = Modifier.weight(1f)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            // The one place the full name matters more than
                            // row-height consistency: 2 lines before ellipsis.
                            Text(
                                text = script.name,
                                style = AppTypography.section,
                                color = AppColors.text,
                                maxLines = 2,
                                overflow = TextOverflow.Ellipsis,
                                modifier = Modifier.weight(1f, fill = false),
                            )
                            if (isInstalled) {
                                Spacer(Modifier.width(AppSpacing.space3))
                                AppBadge(text = stringResource(Res.string.addon_installed_badge), variant = AppBadgeVariant.Success)
                            }
                        }
                        if (script.shortDescription.isNotBlank()) {
                            Spacer(Modifier.height(AppSpacing.space1))
                            Text(
                                text = script.shortDescription,
                                style = AppTypography.caption,
                                color = AppColors.textMuted,
                                maxLines = 2,
                                overflow = TextOverflow.Ellipsis,
                            )
                        }
                    }
                    if (!isNarrow) {
                        Spacer(Modifier.width(AppSpacing.space4))
                        InstallAction(
                            isInstalled = isInstalled,
                            isPending = isPending,
                            onInstall = onInstall,
                            onUninstall = onUninstall,
                            modifier = Modifier.widthIn(min = ActionButtonMinWidth),
                        )
                    }
                }
                if (isNarrow) {
                    Spacer(Modifier.height(AppSpacing.space5))
                    InstallAction(
                        isInstalled = isInstalled,
                        isPending = isPending,
                        onInstall = onInstall,
                        onUninstall = onUninstall,
                        modifier = Modifier.fillMaxWidth(),
                    )
                }
            }

            AppDivider()
            Column(modifier = sectionModifier) {
                SectionHeading(stringResource(Res.string.addon_details_section))
                val fields = listOf(
                    stringResource(Res.string.addon_detail_version) to stringResource(Res.string.script_version, script.version),
                    stringResource(Res.string.addon_detail_updated) to formatDate(script.lastUpdated),
                    stringResource(Res.string.addon_detail_sites) to script.allowedDomains.size.toString(),
                )
                val columns = if (isNarrow) 2 else 3
                fields.chunked(columns).forEachIndexed { index, row ->
                    if (index > 0) Spacer(Modifier.height(AppSpacing.space4))
                    Row(modifier = Modifier.fillMaxWidth()) {
                        row.forEach { (label, value) -> DetailCell(label, value, modifier = Modifier.weight(1f)) }
                        repeat(columns - row.size) { Spacer(Modifier.weight(1f)) }
                    }
                }
            }

            AppDivider()
            Column(modifier = sectionModifier) {
                SectionHeading(stringResource(Res.string.addon_site_access_section))
                SiteAccess(domains = script.allowedDomains, isInstalled = isInstalled)
            }

            if (script.description.isNotBlank()) {
                AppDivider()
                Column(modifier = sectionModifier) {
                    SectionHeading(stringResource(Res.string.addon_about_section))
                    Text(text = script.description, style = AppTypography.body, color = AppColors.text)
                }
            }
        }
    }
}

@Composable
private fun SectionHeading(text: String) {
    Text(text = text, style = AppTypography.section, color = AppColors.text)
    Spacer(Modifier.height(AppSpacing.space3))
}

@Composable
private fun InstallAction(
    isInstalled: Boolean,
    isPending: Boolean,
    onInstall: () -> Unit,
    onUninstall: () -> Unit,
    modifier: Modifier = Modifier,
) {
    if (isInstalled) {
        AppButton(
            text = stringResource(Res.string.addon_uninstall_button),
            onClick = onUninstall,
            variant = AppButtonVariant.Outlined,
            enabled = !isPending,
            loading = isPending,
            modifier = modifier,
        )
    } else {
        AppButton(
            text = stringResource(Res.string.addon_install_button),
            onClick = onInstall,
            variant = AppButtonVariant.Filled,
            enabled = !isPending,
            loading = isPending,
            modifier = modifier,
        )
    }
}

/**
 * The domains this add-on's script may reach, one per row with a globe
 * glyph, prefaced by what that means given the install state ("can access"
 * vs "installing gives access"). Hostnames are exact identifiers, so they're
 * set in the Data role.
 */
@Composable
private fun SiteAccess(domains: List<String>, isInstalled: Boolean) {
    if (domains.isEmpty()) {
        Text(text = stringResource(Res.string.addon_no_domains), style = AppTypography.body, color = AppColors.textMuted)
        return
    }
    Text(
        text = stringResource(if (isInstalled) Res.string.addon_domains_installed else Res.string.addon_domains_not_installed),
        style = AppTypography.caption,
        color = AppColors.textMuted,
    )
    Spacer(Modifier.height(AppSpacing.space2))
    domains.forEach { domain ->
        Row(
            modifier = Modifier.fillMaxWidth().padding(vertical = AppSpacing.space2),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            GlobeGlyph(modifier = Modifier.size(14.dp))
            Spacer(Modifier.width(AppSpacing.space3))
            Text(
                text = domain,
                style = AppTypography.data,
                color = AppColors.text,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
        }
    }
}
