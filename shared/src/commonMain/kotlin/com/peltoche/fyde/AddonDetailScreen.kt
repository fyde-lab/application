package com.peltoche.fyde

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import com.peltoche.fyde.components.action.AppButton
import com.peltoche.fyde.components.action.AppButtonVariant
import com.peltoche.fyde.components.navigation.AppTopBar
import com.peltoche.fyde.resources.Res
import com.peltoche.fyde.resources.addon_domains_section
import com.peltoche.fyde.resources.addon_install_button
import com.peltoche.fyde.resources.addon_uninstall_button
import com.peltoche.fyde.resources.addon_version
import com.peltoche.fyde.resources.back
import com.peltoche.fyde.theme.AppColors
import com.peltoche.fyde.theme.AppSpacing
import com.peltoche.fyde.theme.AppTypography
import org.jetbrains.compose.resources.stringResource

/**
 * Full-screen detail view opened from an [AddonsScreen] card's "View"
 * button, showing [script]'s own description and allowed domains alongside
 * the install/uninstall action that used to live inline on the card itself
 * as an enable/disable switch. Plain top-bar-plus-content shape, same as
 * [PdfViewerScreen], rather than [AppScaffold]'s sidebar, since this is a
 * drill-down from Addons, not a top-level destination of its own.
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
) {
    Column(
        modifier = modifier
            .fillMaxSize()
            .background(AppColors.background),
    ) {
        AppTopBar(title = script.name, onBack = onBack, backContentDescription = stringResource(Res.string.back))

        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(AppSpacing.space4),
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                AddonIcon(icon = script.icon)
                Spacer(Modifier.width(AppSpacing.space4))
                Column {
                    Text(text = script.name, style = AppTypography.title, color = AppColors.text)
                    Spacer(Modifier.height(AppSpacing.space1))
                    Text(
                        text = stringResource(Res.string.addon_version, script.version.toInt()),
                        style = AppTypography.caption,
                        color = AppColors.textMuted,
                    )
                }
            }

            if (script.description.isNotBlank()) {
                Spacer(Modifier.height(AppSpacing.space4))
                Text(text = script.description, style = AppTypography.body, color = AppColors.text)
            }

            if (script.allowedDomains.isNotEmpty()) {
                Spacer(Modifier.height(AppSpacing.space4))
                Text(
                    text = stringResource(Res.string.addon_domains_section),
                    style = AppTypography.micro,
                    color = AppColors.textMuted,
                )
                Spacer(Modifier.height(AppSpacing.space2))
                script.allowedDomains.forEach { domain ->
                    Text(text = domain, style = AppTypography.body, color = AppColors.text)
                }
            }

            Spacer(Modifier.weight(1f))

            if (isInstalled) {
                AppButton(
                    text = stringResource(Res.string.addon_uninstall_button),
                    onClick = onUninstall,
                    variant = AppButtonVariant.Outlined,
                    enabled = !isPending,
                    loading = isPending,
                    modifier = Modifier.fillMaxWidth(),
                )
            } else {
                AppButton(
                    text = stringResource(Res.string.addon_install_button),
                    onClick = onInstall,
                    variant = AppButtonVariant.Filled,
                    enabled = !isPending,
                    loading = isPending,
                    modifier = Modifier.fillMaxWidth(),
                )
            }
        }
    }
}
