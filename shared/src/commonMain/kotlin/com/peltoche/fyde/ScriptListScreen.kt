package com.peltoche.fyde

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.SheetState
import androidx.compose.material3.SheetValue
import androidx.compose.material3.Text
import androidx.compose.material3.rememberBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.peltoche.fyde.components.action.AppTextButton
import com.peltoche.fyde.components.content.AppBadge
import com.peltoche.fyde.components.content.AppBadgeVariant
import com.peltoche.fyde.components.content.AppDivider
import com.peltoche.fyde.components.content.AppListItem
import com.peltoche.fyde.components.feedback.AppBanner
import com.peltoche.fyde.components.feedback.AppEmptyState
import com.peltoche.fyde.components.feedback.AppErrorState
import com.peltoche.fyde.components.feedback.AppLoading
import com.peltoche.fyde.components.feedback.AppSkeleton
import com.peltoche.fyde.components.form.AppSwitch
import com.peltoche.fyde.components.form.AppTextField
import com.peltoche.fyde.components.navigation.AppTopBar
import com.peltoche.fyde.components.overlay.AppBottomSheet
import com.peltoche.fyde.layout.AppContentContainer
import com.peltoche.fyde.layout.AppContentWidth
import com.peltoche.fyde.layout.AppWindowSizeClass
import com.peltoche.fyde.layout.LocalAppWindowSizeClass
import com.peltoche.fyde.resources.Res
import com.peltoche.fyde.resources.cancel
import com.peltoche.fyde.resources.connect_failed_title
import com.peltoche.fyde.resources.connecting_message
import com.peltoche.fyde.resources.create
import com.peltoche.fyde.resources.dismiss
import com.peltoche.fyde.resources.uninstall_script
import com.peltoche.fyde.resources.new_script
import com.peltoche.fyde.resources.new_script_sheet_title
import com.peltoche.fyde.resources.no_scripts_description
import com.peltoche.fyde.resources.no_scripts_title
import com.peltoche.fyde.resources.retry
import com.peltoche.fyde.resources.script_code_label
import com.peltoche.fyde.resources.script_name_label
import com.peltoche.fyde.resources.script_private_badge
import com.peltoche.fyde.resources.script_public_badge
import com.peltoche.fyde.resources.script_public_label
import com.peltoche.fyde.resources.scripts_title
import com.peltoche.fyde.theme.AppColors
import com.peltoche.fyde.theme.AppRadius
import com.peltoche.fyde.theme.AppSpacing
import com.peltoche.fyde.theme.AppTypography
import org.jetbrains.compose.resources.stringResource

/**
 * Everything [ScriptListScreen] needs to render, and nothing it doesn't — no
 * `FydeSdk` types appear here, same separation as [DocumentListUiState].
 */
sealed interface ScriptListUiState {
    /** Not yet known whether the server is reachable; nothing to show at all. */
    data object Connecting : ScriptListUiState

    /** The initial connection failed outright. */
    data class ConnectFailed(val message: String, val onRetry: () -> Unit) : ScriptListUiState

    /**
     * Connected. [scripts] is `null` while the first page is still being
     * fetched (rendered as a skeleton), and non-null once loaded (possibly
     * empty). [banner] surfaces a non-fatal problem (e.g. a failed create or
     * uninstall) without discarding what's already loaded.
     */
    data class Loaded(
        val scripts: List<FydeScript>?,
        val banner: String? = null,
        val onDismissBanner: () -> Unit = {},
    ) : ScriptListUiState
}

@Composable
fun ScriptListScreen(
    state: ScriptListUiState,
    modifier: Modifier = Modifier,
    onOpenDocuments: () -> Unit = {},
    onOpenSettings: () -> Unit = {},
    onOpenAddons: () -> Unit = {},
    onCreateScript: (name: String, isPublic: Boolean, script: String) -> Unit = { _, _, _ -> },
    isCreating: Boolean = false,
    onUninstallScript: (FydeScript) -> Unit = {},
) {
    when (state) {
        ScriptListUiState.Connecting -> AppLoading(
            modifier = modifier,
            message = stringResource(Res.string.connecting_message),
        )
        is ScriptListUiState.ConnectFailed -> AppErrorState(
            modifier = modifier,
            icon = { AlertGlyph(modifier = Modifier.size(32.dp)) },
            title = stringResource(Res.string.connect_failed_title),
            description = state.message,
            actionLabel = stringResource(Res.string.retry),
            onAction = state.onRetry,
        )
        is ScriptListUiState.Loaded -> LoadedScriptList(state, modifier, onOpenDocuments, onOpenSettings, onOpenAddons, onCreateScript, isCreating, onUninstallScript)
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun LoadedScriptList(
    state: ScriptListUiState.Loaded,
    modifier: Modifier = Modifier,
    onOpenDocuments: () -> Unit = {},
    onOpenSettings: () -> Unit = {},
    onOpenAddons: () -> Unit = {},
    onCreateScript: (name: String, isPublic: Boolean, script: String) -> Unit = { _, _, _ -> },
    isCreating: Boolean = false,
    onUninstallScript: (FydeScript) -> Unit = {},
) {
    val windowSizeClass = LocalAppWindowSizeClass.current
    val isDesktop = windowSizeClass == AppWindowSizeClass.Expanded
    var showCreateSheet by remember { mutableStateOf(false) }

    TopLevelScaffold(
        current = SidebarDestination.Scripts,
        onOpenDocuments = onOpenDocuments,
        onOpenScripts = {},
        onOpenAddons = onOpenAddons,
        onOpenSettings = onOpenSettings,
        modifier = modifier,
        topBar = {
            AppTopBar(title = stringResource(Res.string.scripts_title), windowSizeClass = windowSizeClass) {
                AppTextButton(
                    text = stringResource(Res.string.new_script),
                    onClick = { showCreateSheet = true },
                    leadingIcon = {
                        // "+" stands in for an icon glyph, not language.
                        Text("+", style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold)
                    },
                )
            }
        },
    ) { contentPadding ->
        AppContentContainer(
            modifier = Modifier
                .fillMaxSize()
                .padding(contentPadding)
                .background(AppColors.background),
            width = AppContentWidth.List,
            windowSizeClass = windowSizeClass,
            sidebarAdjacent = isDesktop,
        ) {
            Column(modifier = Modifier.fillMaxSize()) {
                state.banner?.let { message ->
                    AppBanner(message = message, onDismiss = state.onDismissBanner, dismissLabel = stringResource(Res.string.dismiss))
                }

                val scripts = state.scripts
                when {
                    scripts == null -> ScriptSkeletonList(modifier = Modifier.weight(1f))

                    scripts.isEmpty() -> AppEmptyState(
                        modifier = Modifier.weight(1f),
                        icon = { CodeGlyph(modifier = Modifier.size(40.dp), tint = AppColors.textMuted, strokeWidth = 2.dp) },
                        title = stringResource(Res.string.no_scripts_title),
                        description = stringResource(Res.string.no_scripts_description),
                    )

                    else -> ScriptList(
                        modifier = Modifier.weight(1f),
                        scripts = scripts,
                        onUninstallScript = onUninstallScript,
                    )
                }
            }
        }
    }

    if (showCreateSheet) {
        CreateScriptSheet(
            sheetState = rememberBottomSheetState(initialValue = SheetValue.Expanded),
            isCreating = isCreating,
            onDismiss = { showCreateSheet = false },
            onCreate = { name, isPublic, script ->
                onCreateScript(name, isPublic, script)
                showCreateSheet = false
            },
        )
    }
}

/**
 * Every script the user has installed — their own and public add-ons alike,
 * since `listUserScripts` doesn't distinguish them. Found via review: an
 * installed add-on showed up here with a "</>" placeholder and a "Disable"
 * button, and on the Add-ons screen with its real logo and "Uninstall" —
 * the same object, two icons, two verbs. Rows now share [AddonIcon] and
 * [scriptVersionLine] with the Add-ons screens, and the action is
 * "Uninstall" in both places.
 */
@Composable
private fun ScriptList(scripts: List<FydeScript>, onUninstallScript: (FydeScript) -> Unit, modifier: Modifier = Modifier) {
    LazyColumn(modifier = modifier.fillMaxSize(), contentPadding = PaddingValues(top = AppSpacing.space2)) {
        items(scripts, key = { it.id }) { script ->
            AppListItem(
                title = script.name,
                supportingText = scriptVersionLine(script),
                supportingTextStyle = AppTypography.data,
                leadingContent = { AddonIcon(icon = script.icon, size = 40.dp) },
                trailingContent = {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        AppBadge(
                            text = stringResource(if (script.isPublic) Res.string.script_public_badge else Res.string.script_private_badge),
                            variant = if (script.isPublic) AppBadgeVariant.Primary else AppBadgeVariant.Neutral,
                        )
                        Spacer(Modifier.width(AppSpacing.space3))
                        AppTextButton(text = stringResource(Res.string.uninstall_script), onClick = { onUninstallScript(script) })
                    }
                },
                modifier = Modifier.heightIn(min = 64.dp),
            )
            AppDivider()
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun CreateScriptSheet(
    sheetState: SheetState,
    isCreating: Boolean,
    onDismiss: () -> Unit,
    onCreate: (name: String, isPublic: Boolean, script: String) -> Unit,
) {
    var name by remember { mutableStateOf("") }
    var script by remember { mutableStateOf("") }
    var isPublic by remember { mutableStateOf(false) }

    AppBottomSheet(onDismissRequest = onDismiss, sheetState = sheetState) {
        Column(modifier = Modifier.fillMaxWidth().padding(AppSpacing.space5)) {
            Text(
                text = stringResource(Res.string.new_script_sheet_title),
                style = AppTypography.section,
                color = AppColors.text,
            )
            Spacer(Modifier.height(AppSpacing.space5))
            AppTextField(
                value = name,
                onValueChange = { name = it },
                label = stringResource(Res.string.script_name_label),
                enabled = !isCreating,
            )
            Spacer(Modifier.height(AppSpacing.space4))
            AppTextField(
                value = script,
                onValueChange = { script = it },
                label = stringResource(Res.string.script_code_label),
                enabled = !isCreating,
                singleLine = false,
                modifier = Modifier.heightIn(min = 160.dp),
            )
            Spacer(Modifier.height(AppSpacing.space4))
            AppSwitch(
                checked = isPublic,
                onCheckedChange = { isPublic = it },
                enabled = !isCreating,
                label = stringResource(Res.string.script_public_label),
            )
            Spacer(Modifier.height(AppSpacing.space6))
            Row(modifier = Modifier.fillMaxWidth()) {
                AppTextButton(text = stringResource(Res.string.cancel), onClick = onDismiss, enabled = !isCreating)
                Spacer(Modifier.width(AppSpacing.space3))
                AppTextButton(
                    text = stringResource(Res.string.create),
                    onClick = { onCreate(name, isPublic, script) },
                    enabled = !isCreating && name.isNotBlank() && script.isNotBlank(),
                    loading = isCreating,
                )
            }
            Spacer(Modifier.height(AppSpacing.space5))
        }
    }
}

@Composable
private fun ScriptSkeletonList(modifier: Modifier = Modifier) {
    Column(modifier = modifier.fillMaxWidth()) {
        repeat(4) { SkeletonScriptRow() }
    }
}

@Composable
private fun SkeletonScriptRow() {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = AppSpacing.space5, vertical = AppSpacing.space4),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        AppSkeleton(modifier = Modifier.size(40.dp), shape = AppRadius.md)
        Spacer(Modifier.width(AppSpacing.space4))
        Column {
            AppSkeleton(modifier = Modifier.size(width = 140.dp, height = 14.dp))
            Spacer(Modifier.height(AppSpacing.space1))
            AppSkeleton(modifier = Modifier.size(width = 90.dp, height = 10.dp))
        }
    }
}
