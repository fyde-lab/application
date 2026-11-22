package com.peltoche.fyde

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.selection.selectableGroup
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Text
import androidx.compose.material3.ripple
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.intl.Locale
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.peltoche.fyde.components.action.AppBackButton
import com.peltoche.fyde.components.action.AppButton
import com.peltoche.fyde.components.content.AppDivider
import com.peltoche.fyde.components.content.AppGroup
import com.peltoche.fyde.components.content.AppGroupCard
import com.peltoche.fyde.components.content.AppGroupRow
import com.peltoche.fyde.components.content.AppSectionTitle
import com.peltoche.fyde.components.content.SealHero
import com.peltoche.fyde.components.feedback.AppErrorNote
import com.peltoche.fyde.components.feedback.AppNote
import com.peltoche.fyde.components.feedback.AppNoteKind
import com.peltoche.fyde.components.form.AppRadio
import com.peltoche.fyde.components.form.AppTextField
import com.peltoche.fyde.components.navigation.AppPageBar
import com.peltoche.fyde.components.navigation.AppTopBar
import com.peltoche.fyde.components.overlay.AppDialog
import com.peltoche.fyde.icons.CheckGlyph
import com.peltoche.fyde.icons.ChevronRightGlyph
import com.peltoche.fyde.icons.EyeGlyph
import com.peltoche.fyde.icons.KeyGlyph
import com.peltoche.fyde.icons.LaptopGlyph
import com.peltoche.fyde.icons.LogoutGlyph
import com.peltoche.fyde.icons.PencilGlyph
import com.peltoche.fyde.icons.ServerGlyph
import com.peltoche.fyde.icons.ShieldGlyph
import com.peltoche.fyde.icons.SlidersGlyph
import com.peltoche.fyde.icons.SunGlyph
import com.peltoche.fyde.layout.AppContentContainer
import com.peltoche.fyde.layout.AppContentWidth
import com.peltoche.fyde.layout.AppTwoPaneLayout
import com.peltoche.fyde.layout.AppWindowSizeClass
import com.peltoche.fyde.layout.LocalAppWindowSizeClass
import com.peltoche.fyde.layout.screenMargin
import com.peltoche.fyde.resources.Res
import com.peltoche.fyde.resources.appearance_text_size_note
import com.peltoche.fyde.resources.cancel
import com.peltoche.fyde.resources.change_password_confirm
import com.peltoche.fyde.resources.change_password_current
import com.peltoche.fyde.resources.change_password_intro
import com.peltoche.fyde.resources.change_password_kit_note
import com.peltoche.fyde.resources.change_password_new
import com.peltoche.fyde.resources.change_password_row_subtitle
import com.peltoche.fyde.resources.change_password_submitting
import com.peltoche.fyde.resources.change_password_title
import com.peltoche.fyde.resources.language_section_title
import com.peltoche.fyde.resources.log_out
import com.peltoche.fyde.resources.log_out_confirm_text
import com.peltoche.fyde.resources.log_out_confirm_title_computer
import com.peltoche.fyde.resources.log_out_confirm_title_device
import com.peltoche.fyde.resources.passwords_mismatch
import com.peltoche.fyde.resources.recovery_kit_row_created
import com.peltoche.fyde.resources.recovery_kit_row_missing
import com.peltoche.fyde.resources.recovery_kit_row_subtitle
import com.peltoche.fyde.resources.recovery_kit_section
import com.peltoche.fyde.resources.security_sealed_text
import com.peltoche.fyde.resources.security_sealed_title
import com.peltoche.fyde.resources.security_server_view
import com.peltoche.fyde.resources.security_server_view_subtitle
import com.peltoche.fyde.resources.server_view_intro
import com.peltoche.fyde.resources.server_view_never_content
import com.peltoche.fyde.resources.server_view_never_credentials
import com.peltoche.fyde.resources.server_view_never_metadata
import com.peltoche.fyde.resources.server_view_never_password
import com.peltoche.fyde.resources.server_view_never_title
import com.peltoche.fyde.resources.server_view_sees_account
import com.peltoche.fyde.resources.server_view_sees_devices
import com.peltoche.fyde.resources.server_view_sees_size
import com.peltoche.fyde.resources.server_view_sees_times
import com.peltoche.fyde.resources.server_view_sees_title
import com.peltoche.fyde.resources.sessions_section_title
import com.peltoche.fyde.resources.settings_appearance
import com.peltoche.fyde.resources.settings_log_out_computer
import com.peltoche.fyde.resources.settings_log_out_device
import com.peltoche.fyde.resources.settings_plan
import com.peltoche.fyde.resources.settings_seal_subtitle
import com.peltoche.fyde.resources.settings_seal_title
import com.peltoche.fyde.resources.settings_security
import com.peltoche.fyde.resources.settings_title
import com.peltoche.fyde.resources.theme_section_title
import com.peltoche.fyde.resources.usage_of_limit
import com.peltoche.fyde.theme.AppAppearance
import com.peltoche.fyde.theme.AppColors
import com.peltoche.fyde.theme.AppDimensions
import com.peltoche.fyde.theme.AppIconSize
import com.peltoche.fyde.theme.AppRadius
import com.peltoche.fyde.theme.AppSpacing
import com.peltoche.fyde.theme.AppThemeOption
import com.peltoche.fyde.theme.AppTypography
import com.peltoche.fyde.theme.appFocusRing
import org.jetbrains.compose.resources.StringResource
import org.jetbrains.compose.resources.stringResource

/**
 * The app's two supported UI languages, each showing its own name in its own
 * language (an "endonym") rather than a translation of it — the standard
 * convention for a language picker, so a reader can find their language
 * regardless of whichever one currently happens to be active. [code] must
 * match one of the resource sets under `commonMain/composeResources`
 * (`values`/`values-fr`).
 */
private enum class AppLanguage(val code: String, val displayName: String) {
    FRENCH("fr", "Français"),
    ENGLISH("en", "English"),
    ;

    companion object {
        fun fromCode(code: String?): AppLanguage? = entries.find { it.code == code }
    }
}

/**
 * The pages Settings opens onto: M1's rows ([Security], [Devices], [Plan],
 * [Appearance]), and [RecoveryKit] (O6's content), [ServerView] (M3) and
 * [ChangePassword] (M11), reached from inside [Security].
 */
enum class SettingsSection(val title: StringResource) {
    Security(Res.string.settings_security),
    RecoveryKit(Res.string.recovery_kit_section),
    ServerView(Res.string.security_server_view),
    ChangePassword(Res.string.change_password_title),
    Devices(Res.string.sessions_section_title),
    Plan(Res.string.settings_plan),
    Appearance(Res.string.settings_appearance),
    ;

    /** The M1 row this page belongs to, and where its back button leads. */
    val parent: SettingsSection?
        get() = when (this) {
            RecoveryKit, ServerView, ChangePassword -> Security
            else -> null
        }
}

/**
 * The Settings destination, design-system.md's Moi (M1–M8).
 *
 * At Compact it's M1: the seal card, then one row per page — Sécurité (M2,
 * leading to the recovery kit, O6's content, Changer le mot de passe, M11,
 * and Ce que voit le serveur, M3),
 * Appareils (M5), Offre et usage
 * (M6), Apparence et langue (M7) — and logging out (M8). Each page pushes
 * inside the tab, under a bar with the 44 back circle. At Medium and Expanded
 * the same menu is the list column of a master–detail split and the page
 * opens beside it, Sécurité until another is picked. [openSection] is the
 * page open, null for the menu; `App.kt` holds it so it outlives the rebuild
 * a language change causes.
 *
 * Left out until the SDK can back them: the profile card (no username
 * getter), M2's device tiles and last-sync times, the password-check row,
 * O6b's check of the kit, Aide, and Voir les offres.
 *
 * M2's recovery kit row reads [recoveryKeyStatus]; its page
 * ([RecoveryKitPage]) shows [recoveryKit], asks for a new key through
 * [onGenerateRecoveryKey] and drops the words through [onRecoveryKitDone].
 *
 * M11 rates the new password with [analyzePassword] as it's typed and
 * submits through [onChangePassword], busy while [isChangingPassword];
 * [changePasswordError] says why it failed.
 *
 * [currentAppearance] is switched by [onAppearanceChange] (see
 * [AppAppearance]), which `App.kt` saves on this device, reporting a failure
 * as [appearanceError]; [currentLanguage] by [onLanguageChange], which `App.kt`
 * saves through `fyde-sdk`'s `set_language`, reporting a failure as
 * [languageError]. [planUsage] and [sessions] feed M6 and M5 (see
 * [PlanUsageSection], [SessionsSection]).
 *
 * Logging out wipes this device's copy of the account (see
 * [FydeSdk.logout]), so it asks first (M8); the dialog stays up, busy, while
 * [isLoggingOut], and [logoutError] reports a failure to clear local state,
 * the one case that keeps the user here.
 */
@Composable
fun SettingsScreen(
    openSection: SettingsSection?,
    onOpenSectionChange: (SettingsSection?) -> Unit,
    currentAppearance: AppAppearance = AppAppearance.Default,
    onAppearanceChange: (AppAppearance) -> Unit = {},
    currentLanguage: String?,
    onLanguageChange: (String) -> Unit,
    navigation: TopLevelNavigation,
    onLogout: () -> Unit = {},
    isLoggingOut: Boolean = false,
    logoutError: String? = null,
    languageError: String? = null,
    appearanceError: String? = null,
    planUsage: PlanUsageUiState = PlanUsageUiState.Loading,
    sessions: SessionsUiState = SessionsUiState.Loading,
    onDisconnectSession: (FydeOpenSession) -> Unit = {},
    disconnectingSessionId: String? = null,
    disconnectSessionError: String? = null,
    analyzePassword: suspend (String) -> Result<FydePasswordAnalysis> = { Result.failure(UnsupportedOperationException()) },
    onChangePassword: (currentPassword: String, newPassword: String) -> Unit = { _, _ -> },
    isChangingPassword: Boolean = false,
    changePasswordError: String? = null,
    recoveryKeyStatus: RecoveryKeyStatus = RecoveryKeyStatus.Loading,
    recoveryKit: RecoveryKitUiState = RecoveryKitUiState.Idle,
    onGenerateRecoveryKey: () -> Unit = {},
    onRecoveryKitDone: () -> Unit = {},
    modifier: Modifier = Modifier,
) {
    var confirmingLogout by remember { mutableStateOf(false) }
    val ui = SettingsUi(
        appearance = currentAppearance,
        onAppearanceChange = onAppearanceChange,
        language = currentLanguage,
        onLanguageChange = onLanguageChange,
        languageError = languageError,
        appearanceError = appearanceError,
        planUsage = planUsage,
        sessions = sessions,
        onDisconnectSession = onDisconnectSession,
        disconnectingSessionId = disconnectingSessionId,
        disconnectSessionError = disconnectSessionError,
        analyzePassword = analyzePassword,
        onChangePassword = onChangePassword,
        isChangingPassword = isChangingPassword,
        changePasswordError = changePasswordError,
        recoveryKeyStatus = recoveryKeyStatus,
        recoveryKit = recoveryKit,
        onGenerateRecoveryKey = onGenerateRecoveryKey,
        onRecoveryKitDone = onRecoveryKitDone,
        isLoggingOut = isLoggingOut,
        logoutError = logoutError,
        onLogOut = { confirmingLogout = true },
        onOpen = onOpenSectionChange,
    )

    TopLevelScaffold(
        // Re-selecting Settings goes back to its menu, like a drill-down's back.
        navigation = navigation.copy(onOpenSettings = { onOpenSectionChange(null) }),
        selection = SidebarSelection.Settings,
        modifier = modifier,
        topBar = {
            val windowSizeClass = LocalAppWindowSizeClass.current
            val section = openSection
            when {
                // At Expanded the title heads the menu column, so the page runs up to the top of the window.
                windowSizeClass == AppWindowSizeClass.Expanded -> Unit
                windowSizeClass == AppWindowSizeClass.Compact && section != null -> DrillDownBar(
                    parentTitle = stringResource(section.parent?.title ?: Res.string.settings_title),
                    onBack = { onOpenSectionChange(section.parent) },
                )
                else -> AppTopBar(title = stringResource(Res.string.settings_title), windowSizeClass = windowSizeClass)
            }
        },
    ) { contentPadding ->
        val windowSizeClass = LocalAppWindowSizeClass.current
        val contentModifier = Modifier.fillMaxSize().padding(contentPadding).background(AppColors.background)
        when (windowSizeClass) {
            AppWindowSizeClass.Compact -> CompactSettings(ui, openSection, contentModifier)
            AppWindowSizeClass.Medium, AppWindowSizeClass.Expanded ->
                WideSettings(ui, openSection ?: SettingsSection.Security, windowSizeClass, contentModifier)
        }
    }

    // A failed logout closes the dialog, so the note under the logout row can say why.
    LaunchedEffect(logoutError) {
        if (logoutError != null) confirmingLogout = false
    }
    if (confirmingLogout) {
        LogoutDialog(
            isLoggingOut = isLoggingOut,
            onConfirm = onLogout,
            onDismiss = { if (!isLoggingOut) confirmingLogout = false },
        )
    }
}

/** What every part of [SettingsScreen] reads, gathered once so the layouts don't each take twenty parameters. */
private class SettingsUi(
    val appearance: AppAppearance,
    val onAppearanceChange: (AppAppearance) -> Unit,
    val language: String?,
    val onLanguageChange: (String) -> Unit,
    val languageError: String?,
    val appearanceError: String?,
    val planUsage: PlanUsageUiState,
    val sessions: SessionsUiState,
    val onDisconnectSession: (FydeOpenSession) -> Unit,
    val disconnectingSessionId: String?,
    val disconnectSessionError: String?,
    val analyzePassword: suspend (String) -> Result<FydePasswordAnalysis>,
    val onChangePassword: (String, String) -> Unit,
    val isChangingPassword: Boolean,
    val changePasswordError: String?,
    val recoveryKeyStatus: RecoveryKeyStatus,
    val recoveryKit: RecoveryKitUiState,
    val onGenerateRecoveryKey: () -> Unit,
    val onRecoveryKitDone: () -> Unit,
    val isLoggingOut: Boolean,
    val logoutError: String?,
    val onLogOut: () -> Unit,
    val onOpen: (SettingsSection) -> Unit,
)

/** M1, or the page pushed over it: the page's title in `display` under [DrillDownBar], then its content. */
@Composable
private fun CompactSettings(ui: SettingsUi, section: SettingsSection?, modifier: Modifier) {
    val menuScroll = rememberScrollState()
    AppContentContainer(modifier = modifier, width = AppContentWidth.Detail) {
        if (section == null) {
            Column(modifier = Modifier.fillMaxSize().verticalScroll(menuScroll)) {
                Spacer(Modifier.height(AppSpacing.space5))
                SettingsMenu(ui, selected = null)
                Spacer(Modifier.height(AppSpacing.space7))
            }
        } else {
            // Each page starts at its top; the menu keeps its own scroll for the way back.
            key(section) {
                if (section == SettingsSection.ChangePassword) {
                    ChangePasswordPage(ui, pinnedButton = true)
                } else {
                    Column(modifier = Modifier.fillMaxSize().verticalScroll(rememberScrollState())) {
                        CompactPageTitle(section)
                        SettingsPage(ui, section)
                        Spacer(Modifier.height(AppSpacing.space7))
                    }
                }
            }
        }
    }
}

@Composable
private fun CompactPageTitle(section: SettingsSection) {
    Spacer(Modifier.height(AppSpacing.space3))
    Text(text = stringResource(section.title), style = AppTypography.display, color = AppColors.text)
    Spacer(Modifier.height(AppSpacing.space5))
}

/**
 * Medium and Expanded: the menu as a list column (with the title at its top
 * at Expanded, where there's no top bar) and [section] beside it. A page
 * reached from another ([SettingsSection.parent]) gets a back button
 * beside its title.
 */
@Composable
private fun WideSettings(ui: SettingsUi, section: SettingsSection, windowSizeClass: AppWindowSizeClass, modifier: Modifier) {
    val menuScroll = rememberScrollState()
    AppContentContainer(modifier = modifier, width = AppContentWidth.List, windowSizeClass = windowSizeClass) {
        AppTwoPaneLayout(
            windowSizeClass = windowSizeClass,
            // The menu's rows are short labels, so Medium's narrower column
            // serves at Expanded too, leaving the room to the page.
            primaryPaneWidth = AppDimensions.twoPaneListWidth,
            primary = {
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .verticalScroll(menuScroll)
                        .padding(end = AppSpacing.space6),
                ) {
                    if (windowSizeClass == AppWindowSizeClass.Expanded) {
                        Spacer(Modifier.height(AppSpacing.space6))
                        Text(
                            text = stringResource(Res.string.settings_title),
                            style = AppTypography.title,
                            color = AppColors.text,
                        )
                    }
                    Spacer(Modifier.height(AppSpacing.space5))
                    SettingsMenu(ui, selected = section.parent ?: section)
                    Spacer(Modifier.height(AppSpacing.space7))
                }
            },
            secondary = {
                key(section) {
                    Column(
                        modifier = Modifier
                            .fillMaxSize()
                            .verticalScroll(rememberScrollState())
                            .padding(start = AppSpacing.space8),
                    ) {
                        Column(modifier = Modifier.widthIn(max = AppDimensions.maxWidthDetail)) {
                            Spacer(Modifier.height(AppSpacing.space6))
                            Row(
                                horizontalArrangement = Arrangement.spacedBy(AppSpacing.space4),
                                verticalAlignment = Alignment.CenterVertically,
                            ) {
                                section.parent?.let { parent -> AppBackButton(onClick = { ui.onOpen(parent) }) }
                                Text(text = stringResource(section.title), style = AppTypography.title, color = AppColors.text)
                            }
                            Spacer(Modifier.height(AppSpacing.space5))
                            SettingsPage(ui, section)
                            Spacer(Modifier.height(AppSpacing.space7))
                        }
                    }
                }
            },
        )
    }
}

@Composable
private fun ColumnScope.SettingsPage(ui: SettingsUi, section: SettingsSection) {
    when (section) {
        SettingsSection.Security -> SecurityPage(
            recoveryKeyStatus = ui.recoveryKeyStatus,
            onOpenRecoveryKit = { ui.onOpen(SettingsSection.RecoveryKit) },
            onOpenChangePassword = { ui.onOpen(SettingsSection.ChangePassword) },
            onOpenServerView = { ui.onOpen(SettingsSection.ServerView) },
        )
        SettingsSection.RecoveryKit -> RecoveryKitPage(
            status = ui.recoveryKeyStatus,
            state = ui.recoveryKit,
            onGenerate = ui.onGenerateRecoveryKey,
            onDone = ui.onRecoveryKitDone,
        )
        SettingsSection.ServerView -> ServerViewPage()
        SettingsSection.ChangePassword -> ChangePasswordPage(ui, pinnedButton = false)
        SettingsSection.Devices -> SessionsSection(
            state = ui.sessions,
            planUsage = ui.planUsage,
            onDisconnect = ui.onDisconnectSession,
            disconnectingId = ui.disconnectingSessionId,
            disconnectError = ui.disconnectSessionError,
        )
        SettingsSection.Plan -> PlanUsageSection(ui.planUsage)
        SettingsSection.Appearance -> AppearancePage(ui)
    }
}

/**
 * A drill-down's bar at Compact (`sources-account.html`'s `.navb`): the 44
 * back circle on the content edge, the page it leads back to named in the
 * middle. The page's own title follows below it, in `display`.
 */
@Composable
private fun DrillDownBar(parentTitle: String, onBack: () -> Unit) {
    AppPageBar(
        title = parentTitle,
        onBack = onBack,
        modifier = Modifier
            .height(AppDimensions.topBarHeight)
            .background(AppColors.background)
            .padding(horizontal = screenMargin(AppWindowSizeClass.Compact)),
    )
}

/**
 * M1's content: the seal card, the four page rows (with the figure each
 * page leads with, once known) and, in a group of its own, logging out.
 * [selected] marks the open page in the wide layouts' list column.
 */
@Composable
private fun SettingsMenu(ui: SettingsUi, selected: SettingsSection?) {
    SealCard(onClick = { ui.onOpen(SettingsSection.Security) })
    Spacer(Modifier.height(AppSpacing.space4))
    val usage = (ui.planUsage as? PlanUsageUiState.Loaded)?.usage
    AppGroup(modifier = if (selected != null) Modifier.selectableGroup() else Modifier) {
        SettingsLinkRow(
            label = stringResource(Res.string.settings_security),
            icon = { KeyGlyph(modifier = it) },
            onClick = { ui.onOpen(SettingsSection.Security) },
            selected = selected?.let { it == SettingsSection.Security },
        )
        AppDivider()
        SettingsLinkRow(
            label = stringResource(Res.string.sessions_section_title),
            icon = { LaptopGlyph(modifier = it) },
            onClick = { ui.onOpen(SettingsSection.Devices) },
            value = usage?.let { stringResource(Res.string.usage_of_limit, it.openSessions.toString(), it.maxSessions.toString()) },
            selected = selected?.let { it == SettingsSection.Devices },
        )
        AppDivider()
        SettingsLinkRow(
            label = stringResource(Res.string.settings_plan),
            icon = { SlidersGlyph(modifier = it) },
            onClick = { ui.onOpen(SettingsSection.Plan) },
            value = usage?.let { localizedSize(it.totalSize) },
            selected = selected?.let { it == SettingsSection.Plan },
        )
        AppDivider()
        SettingsLinkRow(
            label = stringResource(Res.string.settings_appearance),
            icon = { SunGlyph(modifier = it) },
            onClick = { ui.onOpen(SettingsSection.Appearance) },
            value = stringResource(ui.appearance.label),
            selected = selected?.let { it == SettingsSection.Appearance },
        )
    }
    Spacer(Modifier.height(AppSpacing.space4))
    AppGroup {
        SettingsLinkRow(
            label = stringResource(
                if (getPlatform().isDesktop) Res.string.settings_log_out_computer else Res.string.settings_log_out_device,
            ),
            icon = { LogoutGlyph(modifier = it) },
            onClick = ui.onLogOut,
            labelColor = AppColors.error,
            showChevron = false,
            enabled = !ui.isLoggingOut,
        )
    }
    ui.logoutError?.let { message ->
        Spacer(Modifier.height(AppSpacing.space4))
        AppErrorNote(message)
    }
}

/**
 * M1's seal card, on a `primary` wash: the vault is sealed, and why that
 * holds. Says nothing about sync — fyde-sdk doesn't report when each
 * device last synced. Opens Sécurité.
 */
@Composable
private fun SealCard(onClick: () -> Unit) {
    val interactionSource = remember { MutableInteractionSource() }
    Row(
        horizontalArrangement = Arrangement.spacedBy(AppSpacing.space4),
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier
            .fillMaxWidth()
            .clip(AppRadius.lg)
            .background(AppColors.tintOf(AppColors.primary))
            .appFocusRing(interactionSource, shape = AppRadius.lg)
            .clickable(interactionSource = interactionSource, indication = ripple(), role = Role.Button, onClick = onClick)
            .padding(AppSpacing.space5),
    ) {
        // 24, off AppIconSize's scale: the mockup's `.ic.lg`, as in SessionLimitScreen.
        ShieldGlyph(modifier = Modifier.size(AppIconSize.prominent), tint = AppColors.success)
        Column(modifier = Modifier.weight(1f)) {
            Text(text = stringResource(Res.string.settings_seal_title), style = AppTypography.bodyEmphasis, color = AppColors.text)
            Text(text = stringResource(Res.string.settings_seal_subtitle), style = AppTypography.caption, color = AppColors.textMuted)
        }
        ChevronRightGlyph(modifier = Modifier.size(AppIconSize.xs))
    }
}

/**
 * One row of a [AppGroup] that opens something: [icon] (handed its
 * size), [label] over an optional [supporting] line, a muted trailing
 * [value], a chevron. [selected] is null where the row only navigates (M1
 * on a phone); otherwise it's one choice of the wide layouts' list column,
 * filled with `selectionSurface` while its page is open.
 */
@Composable
private fun SettingsLinkRow(
    label: String,
    icon: @Composable (Modifier) -> Unit,
    onClick: () -> Unit,
    value: String? = null,
    supporting: String? = null,
    selected: Boolean? = null,
    labelColor: Color = AppColors.text,
    showChevron: Boolean = true,
    enabled: Boolean = true,
) {
    AppGroupRow(
        minHeight = if (supporting != null) AppDimensions.rowHeightStandard else AppDimensions.rowHeightCompact,
        onClick = onClick,
        selected = selected,
        enabled = enabled,
    ) {
        icon(Modifier.size(AppIconSize.md))
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = label,
                style = if (supporting != null) AppTypography.bodyEmphasis else AppTypography.body,
                color = labelColor,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
            supporting?.let { Text(text = it, style = AppTypography.caption, color = AppColors.textMuted) }
        }
        value?.let { Text(text = it, style = AppTypography.caption, color = AppColors.textMuted, maxLines = 1) }
        if (showChevron) ChevronRightGlyph(modifier = Modifier.size(AppIconSize.xs))
    }
}

/**
 * M2 as far as the SDK backs it: the seal, what it means, and the ways to
 * the recovery kit (its date, or "À créer" in `warning` without one), M11
 * and what the server sees.
 */
@Composable
private fun SecurityPage(
    recoveryKeyStatus: RecoveryKeyStatus,
    onOpenRecoveryKit: () -> Unit,
    onOpenChangePassword: () -> Unit,
    onOpenServerView: () -> Unit,
) {
    Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.fillMaxWidth()) {
        SealHero()
        Spacer(Modifier.height(AppSpacing.space4))
        Text(
            text = stringResource(Res.string.security_sealed_title),
            style = AppTypography.title,
            color = AppColors.text,
            textAlign = TextAlign.Center,
        )
        Spacer(Modifier.height(AppSpacing.space3))
        Text(
            text = stringResource(Res.string.security_sealed_text),
            style = AppTypography.body,
            color = AppColors.textMuted,
            textAlign = TextAlign.Center,
        )
    }
    Spacer(Modifier.height(AppSpacing.space7))
    AppGroup {
        val missingKit = recoveryKeyStatus == RecoveryKeyStatus.Missing
        SettingsLinkRow(
            label = stringResource(Res.string.recovery_kit_section),
            supporting = when (recoveryKeyStatus) {
                RecoveryKeyStatus.Missing -> stringResource(Res.string.recovery_kit_row_missing)
                is RecoveryKeyStatus.Created ->
                    stringResource(Res.string.recovery_kit_row_created, localizedDate(recoveryKeyStatus.epochSeconds))
                RecoveryKeyStatus.Loading, RecoveryKeyStatus.Unknown -> stringResource(Res.string.recovery_kit_row_subtitle)
            },
            icon = { KeyGlyph(modifier = it, tint = if (missingKit) AppColors.warning else AppColors.primary) },
            onClick = onOpenRecoveryKit,
        )
        AppDivider()
        SettingsLinkRow(
            label = stringResource(Res.string.change_password_title),
            supporting = stringResource(Res.string.change_password_row_subtitle),
            icon = { PencilGlyph(modifier = it, tint = AppColors.primary) },
            onClick = onOpenChangePassword,
        )
        AppDivider()
        SettingsLinkRow(
            label = stringResource(Res.string.security_server_view),
            supporting = stringResource(Res.string.security_server_view_subtitle),
            icon = { ServerGlyph(modifier = it, tint = AppColors.primary) },
            onClick = onOpenServerView,
        )
    }
}

/**
 * M11: the current password, the new one with the SDK's strength meter, its
 * confirmation (DO3b's mismatch rule), and a note that the recovery kit
 * stays valid. Submitting waits for the SDK's verdict on what's typed and
 * refuses a password it rates weak, as it would. At Compact ([pinnedButton])
 * the fields scroll above a button pinned at the foot; beside the menu the
 * button follows the note.
 */
@Composable
private fun ChangePasswordPage(ui: SettingsUi, pinnedButton: Boolean) {
    var current by remember { mutableStateOf("") }
    var new by remember { mutableStateOf("") }
    var confirm by remember { mutableStateOf("") }
    var confirmBlurred by remember { mutableStateOf(false) }
    var submitAttempted by remember { mutableStateOf(false) }
    var analyzedFor by remember { mutableStateOf<String?>(null) }
    var analysis by remember { mutableStateOf<FydePasswordAnalysis?>(null) }
    LaunchedEffect(new) {
        if (new.isEmpty()) {
            analysis = null
        } else {
            // Without a verdict (the SDK failed to give one), the SDK still checks on submit.
            analysis = ui.analyzePassword(new).getOrNull()
        }
        analyzedFor = new
    }

    val busy = ui.isChangingPassword
    val matches = confirm == new
    val accepted = analyzedFor == new && analysis?.isAccepted != false
    val canSubmit = !busy && current.isNotEmpty() && new.isNotEmpty() && matches && accepted
    val showMismatch = (confirmBlurred || submitAttempted) && confirm.isNotEmpty() && !matches
    val submit = {
        submitAttempted = true
        if (canSubmit) ui.onChangePassword(current, new)
    }

    val fields: @Composable () -> Unit = {
        Text(text = stringResource(Res.string.change_password_intro), style = AppTypography.body, color = AppColors.textMuted)
        ui.changePasswordError?.let { message ->
            Spacer(Modifier.height(AppSpacing.space5))
            AppErrorNote(message)
        }
        Spacer(Modifier.height(AppSpacing.space6))
        AppTextField(
            value = current,
            onValueChange = { current = it },
            label = stringResource(Res.string.change_password_current),
            enabled = !busy,
            isPassword = true,
            keyboardOptions = KeyboardOptions(imeAction = ImeAction.Next),
        )
        Spacer(Modifier.height(AppSpacing.space5))
        AppTextField(
            value = new,
            onValueChange = { new = it },
            label = stringResource(Res.string.change_password_new),
            enabled = !busy,
            isPassword = true,
            keyboardOptions = KeyboardOptions(imeAction = ImeAction.Next),
        )
        analysis?.takeIf { new.isNotEmpty() }?.let {
            Spacer(Modifier.height(AppSpacing.space3))
            PasswordStrengthMeter(it.strength.toPasswordStrength(), it.length)
        }
        Spacer(Modifier.height(AppSpacing.space5))
        AppTextField(
            value = confirm,
            onValueChange = { confirm = it },
            label = stringResource(Res.string.change_password_confirm),
            enabled = !busy,
            isPassword = true,
            revealable = false,
            trailingIcon = if (confirm.isNotEmpty() && matches) {
                { CheckGlyph(modifier = Modifier.size(AppIconSize.md)) }
            } else {
                null
            },
            errorText = if (showMismatch) stringResource(Res.string.passwords_mismatch) else null,
            modifier = Modifier.onFocusChanged { if (!it.hasFocus && confirm.isNotEmpty()) confirmBlurred = true },
            keyboardOptions = KeyboardOptions(imeAction = ImeAction.Go),
            keyboardActions = KeyboardActions(onGo = { submit() }),
        )
        Spacer(Modifier.height(AppSpacing.space5))
        AppNote(
            message = stringResource(Res.string.change_password_kit_note),
            kind = AppNoteKind.Ok,
            icon = { tint -> KeyGlyph(modifier = Modifier.size(AppIconSize.md), tint = tint) },
        )
    }
    val button: @Composable () -> Unit = {
        AppButton(
            text = stringResource(Res.string.change_password_title),
            onClick = submit,
            enabled = canSubmit,
            loading = busy,
            loadingText = stringResource(Res.string.change_password_submitting),
            modifier = Modifier.fillMaxWidth(),
        )
    }

    if (pinnedButton) {
        Column(modifier = Modifier.fillMaxSize()) {
            Column(modifier = Modifier.weight(1f).fillMaxWidth().verticalScroll(rememberScrollState())) {
                CompactPageTitle(SettingsSection.ChangePassword)
                fields()
                Spacer(Modifier.height(AppSpacing.space6))
            }
            Box(modifier = Modifier.fillMaxWidth().padding(top = AppSpacing.space4, bottom = AppSpacing.space5)) {
                button()
            }
        }
    } else {
        Column(modifier = Modifier.fillMaxWidth()) {
            fields()
            Spacer(Modifier.height(AppSpacing.space6))
            button()
        }
    }
}

private fun FydePasswordStrength.toPasswordStrength(): PasswordStrength = when (this) {
    FydePasswordStrength.Weak -> PasswordStrength.Weak
    FydePasswordStrength.Fair -> PasswordStrength.Fair
    FydePasswordStrength.Strong -> PasswordStrength.Strong
    FydePasswordStrength.VeryStrong -> PasswordStrength.VeryStrong
}

/** M3: what the server can never read, then what it does see, in plain words. */
@Composable
private fun ServerViewPage() {
    Text(text = stringResource(Res.string.server_view_intro), style = AppTypography.body, color = AppColors.textMuted)
    Spacer(Modifier.height(AppSpacing.space6))
    ServerViewCard(
        title = stringResource(Res.string.server_view_never_title),
        accent = AppColors.success,
        icon = { EyeGlyph(modifier = it, tint = AppColors.success, crossed = true) },
        items = listOf(
            Res.string.server_view_never_content,
            Res.string.server_view_never_metadata,
            Res.string.server_view_never_credentials,
            Res.string.server_view_never_password,
        ),
    )
    Spacer(Modifier.height(AppSpacing.space4))
    ServerViewCard(
        title = stringResource(Res.string.server_view_sees_title),
        accent = AppColors.text,
        icon = { EyeGlyph(modifier = it, tint = AppColors.textMuted) },
        items = listOf(
            Res.string.server_view_sees_account,
            Res.string.server_view_sees_size,
            Res.string.server_view_sees_times,
            Res.string.server_view_sees_devices,
        ),
    )
}

@Composable
private fun ServerViewCard(title: String, accent: Color, icon: @Composable (Modifier) -> Unit, items: List<StringResource>) {
    AppGroupCard {
        Row(horizontalArrangement = Arrangement.spacedBy(AppSpacing.space3), verticalAlignment = Alignment.CenterVertically) {
            icon(Modifier.size(AppIconSize.md))
            Text(text = title, style = AppTypography.bodyEmphasis, color = accent)
        }
        Spacer(Modifier.height(AppSpacing.space3))
        items.forEach { item ->
            Text(
                text = stringResource(item),
                style = AppTypography.body,
                color = AppColors.text,
                modifier = Modifier.padding(vertical = AppSpacing.space2),
            )
        }
    }
}

/**
 * M7: the light/dark appearance as three preview tiles, then the language
 * as a radio group. On phones, a line saying the text size follows the
 * system's — desktops have no such setting for it to follow.
 */
@Composable
private fun AppearancePage(ui: SettingsUi) {
    AppSectionTitle(stringResource(Res.string.theme_section_title), first = true)
    Row(
        horizontalArrangement = Arrangement.spacedBy(AppSpacing.space3),
        modifier = Modifier.fillMaxWidth().selectableGroup(),
    ) {
        AppAppearance.entries.forEach { appearance ->
            AppearanceTile(
                appearance = appearance,
                selected = appearance == ui.appearance,
                onClick = { ui.onAppearanceChange(appearance) },
                modifier = Modifier.weight(1f),
            )
        }
    }
    ui.appearanceError?.let { message ->
        Spacer(Modifier.height(AppSpacing.space4))
        AppErrorNote(message)
    }
    AppSectionTitle(stringResource(Res.string.language_section_title))
    // With no saved preference the app follows the platform locale, so that's
    // the one ticked rather than none.
    val currentLanguage = AppLanguage.fromCode(ui.language)
        ?: AppLanguage.fromCode(Locale.current.language)
        ?: AppLanguage.ENGLISH
    AppGroup(modifier = Modifier.selectableGroup()) {
        AppLanguage.entries.forEachIndexed { index, language ->
            if (index > 0) AppDivider()
            LanguageChoice(
                language = language,
                selected = language == currentLanguage,
                onClick = { ui.onLanguageChange(language.code) },
            )
        }
    }
    ui.languageError?.let { message ->
        Spacer(Modifier.height(AppSpacing.space4))
        AppErrorNote(message)
    }
    if (!getPlatform().isDesktop) {
        Spacer(Modifier.height(AppSpacing.space4))
        Text(
            text = stringResource(Res.string.appearance_text_size_note),
            style = AppTypography.caption,
            color = AppColors.textMuted,
        )
    }
}

/** M7's preview tile height — a cosmetic size of the tile alone. */
private val AppearanceTileHeight = 92.dp

/**
 * One appearance: a tile filled with the palette's own `background` —
 * split light and dark for [AppAppearance.System] — over its name. Selected,
 * the tile takes a `primary` outline and its name the text colour.
 */
@Composable
private fun AppearanceTile(appearance: AppAppearance, selected: Boolean, onClick: () -> Unit, modifier: Modifier = Modifier) {
    val interactionSource = remember { MutableInteractionSource() }
    val light = AppThemeOption.Default.colorScheme(darkTheme = false).background
    val dark = AppThemeOption.Default.colorScheme(darkTheme = true).background
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = modifier
            .selectable(
                selected = selected,
                interactionSource = interactionSource,
                indication = null,
                role = Role.RadioButton,
                onClick = onClick,
            ),
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .height(AppearanceTileHeight)
                .appFocusRing(interactionSource, shape = AppRadius.lg)
                .clip(AppRadius.lg)
                .border(
                    width = if (selected) AppDimensions.borderFocus else AppDimensions.borderHairline,
                    color = if (selected) AppColors.primary else AppColors.border,
                    shape = AppRadius.lg,
                ),
        ) {
            val fills = when (appearance) {
                AppAppearance.System -> listOf(light, dark)
                AppAppearance.Light -> listOf(light)
                AppAppearance.Dark -> listOf(dark)
            }
            fills.forEach { fill -> Box(Modifier.weight(1f).fillMaxHeight().background(fill)) }
        }
        Spacer(Modifier.height(AppSpacing.space2))
        Text(
            text = stringResource(appearance.label),
            style = AppTypography.caption,
            color = if (selected) AppColors.text else AppColors.textMuted,
        )
    }
}

/** One language of M7's radio group; the whole row is the radio button. */
@Composable
private fun LanguageChoice(language: AppLanguage, selected: Boolean, onClick: () -> Unit) {
    val interactionSource = remember { MutableInteractionSource() }
    Row(
        horizontalArrangement = Arrangement.spacedBy(AppSpacing.space4),
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier
            .fillMaxWidth()
            .selectable(
                selected = selected,
                interactionSource = interactionSource,
                indication = ripple(),
                role = Role.RadioButton,
                onClick = onClick,
            )
            .appFocusRing(interactionSource, shape = AppRadius.none)
            .heightIn(min = AppDimensions.rowHeightCompact)
            .padding(horizontal = AppSpacing.space5, vertical = AppSpacing.space3),
    ) {
        Text(text = language.displayName, style = AppTypography.body, color = AppColors.text, modifier = Modifier.weight(1f))
        AppRadio(selected = selected)
    }
}

/** M8: what logging out erases here and what stays; confirm filled `error`, busy until it's done. */
@Composable
private fun LogoutDialog(isLoggingOut: Boolean, onConfirm: () -> Unit, onDismiss: () -> Unit) {
    AppDialog(
        onDismissRequest = onDismiss,
        title = stringResource(
            if (getPlatform().isDesktop) Res.string.log_out_confirm_title_computer else Res.string.log_out_confirm_title_device,
        ),
        text = stringResource(Res.string.log_out_confirm_text),
        confirmLabel = stringResource(Res.string.log_out),
        onConfirm = onConfirm,
        dismissLabel = stringResource(Res.string.cancel),
        onDismiss = onDismiss,
        destructive = true,
        confirmLoading = isLoggingOut,
    )
}
