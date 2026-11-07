package com.peltoche.fyde

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeContentPadding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.text.input.ImeAction
import com.peltoche.fyde.components.action.AppBackButton
import com.peltoche.fyde.components.action.AppButton
import com.peltoche.fyde.components.action.AppTextButton
import com.peltoche.fyde.components.feedback.AppNote
import com.peltoche.fyde.components.feedback.AppNoteKind
import com.peltoche.fyde.components.form.AppTextField
import com.peltoche.fyde.icons.AlertGlyph
import com.peltoche.fyde.icons.ClockGlyph
import com.peltoche.fyde.icons.KeyGlyph
import com.peltoche.fyde.layout.AppWindowSizeClass
import com.peltoche.fyde.layout.CenteredScrollColumn
import com.peltoche.fyde.layout.LocalAppWindowSizeClass
import com.peltoche.fyde.resources.Res
import com.peltoche.fyde.resources.login_create_vault
import com.peltoche.fyde.resources.login_key_derivation_computer
import com.peltoche.fyde.resources.login_key_derivation_device
import com.peltoche.fyde.resources.login_no_vault
import com.peltoche.fyde.resources.login_password_help
import com.peltoche.fyde.resources.login_submit
import com.peltoche.fyde.resources.login_submitting
import com.peltoche.fyde.resources.login_title
import com.peltoche.fyde.resources.password_label
import com.peltoche.fyde.resources.username_label
import com.peltoche.fyde.theme.AppColors
import com.peltoche.fyde.theme.AppDimensions
import com.peltoche.fyde.theme.AppIconSize
import com.peltoche.fyde.theme.AppSpacing
import com.peltoche.fyde.theme.AppTypography
import org.jetbrains.compose.resources.stringResource

/**
 * Connexion: back to [WelcomeScreen], "Open my vault", the username and
 * password (with its eye, and a line saying it never leaves the device),
 * then Open the vault, and a link to [RegisterScreen]. Pure presentation —
 * no `FydeSdk` import — so it can be redesigned without touching auth
 * wiring.
 *
 * At Compact it's O2 (`design/assets/screens/O2-connexion.png`), the link
 * pinned at the bottom. At Medium and Expanded it's DO2
 * (`design/assets/screens/DO2-connexion.png`): [SignInSplitLayout]'s brand
 * panel beside a centred form, back at the pane's top left and the link at
 * its top right. The typed username and password live here, above both, so
 * resizing the window across Compact keeps them.
 *
 * [noticeMessage] says why the user is here when it isn't by choice (the
 * session expired), as a warning note; [errorMessage] says why the last
 * attempt failed, as an error note. While [isSubmitting], the fields are
 * disabled and the button spins: key stretching takes seconds.
 *
 * A device without an OS credential store is handled by `App.kt`, which
 * covers this form with [InsecureStorageSheet] (O4, DO4b) on submit.
 */
@Composable
fun LoginScreen(
    onLogIn: (username: String, password: String) -> Unit,
    onNavigateToRegister: () -> Unit,
    onBack: () -> Unit,
    modifier: Modifier = Modifier,
    noticeMessage: String? = null,
    errorMessage: String? = null,
    isSubmitting: Boolean = false,
) {
    var username by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }
    val form = LoginFormState(
        username = username,
        onUsernameChange = { username = it },
        password = password,
        onPasswordChange = { password = it },
        noticeMessage = noticeMessage,
        errorMessage = errorMessage,
        isSubmitting = isSubmitting,
        onLogIn = onLogIn,
    )

    when (LocalAppWindowSizeClass.current) {
        AppWindowSizeClass.Compact -> CompactLogin(form, onNavigateToRegister, onBack, modifier)
        AppWindowSizeClass.Medium, AppWindowSizeClass.Expanded -> SignInSplitLayout(modifier) { paneModifier ->
            WideLoginPane(form, onNavigateToRegister, onBack, paneModifier)
        }
    }
}

/** What both layouts of [LoginScreen] show and edit, and when the form can be submitted. */
private class LoginFormState(
    val username: String,
    val onUsernameChange: (String) -> Unit,
    val password: String,
    val onPasswordChange: (String) -> Unit,
    val noticeMessage: String?,
    val errorMessage: String?,
    val isSubmitting: Boolean,
    private val onLogIn: (username: String, password: String) -> Unit,
) {
    val canSubmit: Boolean get() = !isSubmitting && username.isNotBlank() && password.isNotBlank()

    fun submit() {
        if (canSubmit) onLogIn(username, password)
    }
}

/** O2. The fields scroll when the window is too short; the link to [RegisterScreen] stays pinned at the bottom. */
@Composable
private fun CompactLogin(
    form: LoginFormState,
    onNavigateToRegister: () -> Unit,
    onBack: () -> Unit,
    modifier: Modifier,
) {
    val usernameFocus = remember { FocusRequester() }

    // Land the cursor in the first field on arrival, so the form can be
    // filled and submitted from the keyboard alone.
    LaunchedEffect(Unit) { usernameFocus.requestFocus() }

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(AppColors.background)
            .safeContentPadding(),
        contentAlignment = Alignment.TopCenter,
    ) {
        Column(
            modifier = Modifier
                .widthIn(max = AppDimensions.maxWidthDetail)
                .fillMaxSize(),
        ) {
            Column(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState())
                    .padding(horizontal = AppSpacing.space6),
            ) {
                Spacer(Modifier.height(AppSpacing.space2))
                AppBackButton(onClick = onBack, enabled = !form.isSubmitting)
                Spacer(Modifier.height(AppSpacing.space4))
                Text(
                    text = stringResource(Res.string.login_title),
                    style = AppTypography.display,
                    color = AppColors.text,
                )
                LoginNotes(form)
                Spacer(Modifier.height(AppSpacing.space6))
                LoginFields(form, usernameFocus, showPasswordHelp = true)
                Spacer(Modifier.height(AppSpacing.space6))
                AppButton(
                    text = stringResource(Res.string.login_submit),
                    onClick = form::submit,
                    enabled = form.canSubmit,
                    loading = form.isSubmitting,
                    modifier = Modifier.fillMaxWidth(),
                )
                Spacer(Modifier.height(AppSpacing.space6))
            }
            Row(
                horizontalArrangement = Arrangement.Center,
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = AppSpacing.space6)
                    .padding(bottom = AppSpacing.space5),
            ) {
                NoVaultLink(onNavigateToRegister, enabled = !form.isSubmitting)
            }
        }
    }
}

/**
 * DO2's form pane: back at the top left and the link to [RegisterScreen] at
 * the top right, both hidden while the vault opens (DO2b), around the form
 * centred in an [AppDimensions.authFormColumn] column.
 *
 * While submitting, an info note under the button explains the key
 * derivation that makes it take seconds (DO2b). A
 * failed attempt (DO2c) empties the password and puts the cursor back in
 * it, so the button stays disabled until it's typed again.
 */
@Composable
private fun WideLoginPane(
    form: LoginFormState,
    onNavigateToRegister: () -> Unit,
    onBack: () -> Unit,
    modifier: Modifier,
) {
    val usernameFocus = remember { FocusRequester() }
    val passwordFocus = remember { FocusRequester() }
    val isDesktop = getPlatform().isDesktop

    LaunchedEffect(Unit) { usernameFocus.requestFocus() }
    // Keyed on both so it runs once per failure, after the fields are
    // enabled again — a disabled field can't take focus.
    LaunchedEffect(form.errorMessage, form.isSubmitting) {
        if (form.errorMessage != null && !form.isSubmitting) {
            form.onPasswordChange("")
            passwordFocus.requestFocus()
        }
    }

    Box(modifier = modifier.background(AppColors.background).safeContentPadding()) {
        CenteredScrollColumn(
            modifier = Modifier.fillMaxSize(),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Column(
                modifier = Modifier
                    .widthIn(max = AppDimensions.authFormColumn)
                    .fillMaxWidth()
                    // Clears the back button and the link above the form when the window is short.
                    .padding(horizontal = AppSpacing.space7, vertical = AppSpacing.space10 + AppSpacing.space8),
            ) {
                Text(
                    text = stringResource(Res.string.login_title),
                    style = AppTypography.display,
                    color = AppColors.text,
                )
                LoginNotes(form)
                Spacer(Modifier.height(AppSpacing.space6))
                LoginFields(
                    form,
                    usernameFocus,
                    passwordFocus = passwordFocus,
                    showPasswordHelp = form.errorMessage == null && !form.isSubmitting,
                )
                Spacer(Modifier.height(AppSpacing.space6))
                AppButton(
                    text = stringResource(Res.string.login_submit),
                    onClick = form::submit,
                    enabled = form.canSubmit,
                    loading = form.isSubmitting,
                    loadingText = stringResource(Res.string.login_submitting),
                    modifier = Modifier.fillMaxWidth(),
                )
                if (form.isSubmitting) {
                    Spacer(Modifier.height(AppSpacing.space4))
                    AppNote(
                        message = stringResource(
                            if (isDesktop) Res.string.login_key_derivation_computer else Res.string.login_key_derivation_device,
                        ),
                        kind = AppNoteKind.Info,
                        icon = { tint -> KeyGlyph(modifier = Modifier.size(AppIconSize.md), tint = tint) },
                    )
                }
            }
        }
        if (!form.isSubmitting) {
            AppBackButton(
                onClick = onBack,
                modifier = Modifier
                    .align(Alignment.TopStart)
                    .padding(start = AppSpacing.space9, top = AppSpacing.space8),
            )
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier
                    .align(Alignment.TopEnd)
                    .padding(end = AppSpacing.space9, top = AppSpacing.space8),
            ) {
                NoVaultLink(onNavigateToRegister, enabled = true)
            }
        }
    }
}

/** Why the user is here (a warning note) and why the last attempt failed (an error note), when they apply. */
@Composable
private fun LoginNotes(form: LoginFormState) {
    if (form.noticeMessage != null) {
        Spacer(Modifier.height(AppSpacing.space5))
        AppNote(message = form.noticeMessage, kind = AppNoteKind.Warning, icon = { tint ->
            ClockGlyph(modifier = Modifier.size(AppIconSize.md), tint = tint)
        })
    }
    if (form.errorMessage != null) {
        Spacer(Modifier.height(AppSpacing.space5))
        AppNote(message = form.errorMessage, kind = AppNoteKind.Error, icon = { tint ->
            AlertGlyph(modifier = Modifier.size(AppIconSize.sm), tint = tint)
        })
    }
}

/**
 * The username and password fields. Enter (or the soft keyboard's action
 * key) moves on to the next field, then submits from the last one — but
 * only once the form is complete, same as the button's `enabled` state.
 */
@Composable
private fun LoginFields(
    form: LoginFormState,
    usernameFocus: FocusRequester,
    showPasswordHelp: Boolean,
    passwordFocus: FocusRequester? = null,
) {
    AppTextField(
        enabled = !form.isSubmitting,
        value = form.username,
        onValueChange = form.onUsernameChange,
        label = stringResource(Res.string.username_label),
        modifier = Modifier.focusRequester(usernameFocus),
        keyboardOptions = KeyboardOptions(imeAction = ImeAction.Next),
    )
    Spacer(Modifier.height(AppSpacing.space5))
    AppTextField(
        enabled = !form.isSubmitting,
        value = form.password,
        onValueChange = form.onPasswordChange,
        label = stringResource(Res.string.password_label),
        supportingText = if (showPasswordHelp) stringResource(Res.string.login_password_help) else null,
        isPassword = true,
        modifier = if (passwordFocus != null) Modifier.focusRequester(passwordFocus) else Modifier,
        keyboardOptions = KeyboardOptions(imeAction = ImeAction.Go),
        keyboardActions = KeyboardActions(onGo = { form.submit() }),
    )
}

/** "No vault yet? Create a vault", to [RegisterScreen]. */
@Composable
private fun NoVaultLink(onNavigateToRegister: () -> Unit, enabled: Boolean) {
    Text(
        text = stringResource(Res.string.login_no_vault),
        style = AppTypography.body,
        color = AppColors.textMuted,
    )
    AppTextButton(
        text = stringResource(Res.string.login_create_vault),
        onClick = onNavigateToRegister,
        enabled = enabled,
    )
}

