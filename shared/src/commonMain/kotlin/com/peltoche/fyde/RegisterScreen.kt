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
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
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
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.withStyle
import com.peltoche.fyde.components.action.AppBackButton
import com.peltoche.fyde.components.action.AppButton
import com.peltoche.fyde.components.action.AppTextButton
import com.peltoche.fyde.components.feedback.AppNote
import com.peltoche.fyde.components.feedback.AppNoteKind
import com.peltoche.fyde.components.feedback.AppSegmentedMeter
import com.peltoche.fyde.components.form.AppCheckbox
import com.peltoche.fyde.components.form.AppTextField
import com.peltoche.fyde.icons.AlertGlyph
import com.peltoche.fyde.icons.CheckGlyph
import com.peltoche.fyde.icons.KeyGlyph
import com.peltoche.fyde.layout.AppWindowSizeClass
import com.peltoche.fyde.layout.CenteredScrollColumn
import com.peltoche.fyde.layout.LocalAppWindowSizeClass
import com.peltoche.fyde.resources.Res
import com.peltoche.fyde.resources.confirm_password_label
import com.peltoche.fyde.resources.create_account_title
import com.peltoche.fyde.resources.has_account_prompt
import com.peltoche.fyde.resources.log_in
import com.peltoche.fyde.resources.login_key_derivation_computer
import com.peltoche.fyde.resources.login_key_derivation_device
import com.peltoche.fyde.resources.password_label
import com.peltoche.fyde.resources.password_strength_fair
import com.peltoche.fyde.resources.password_strength_strong
import com.peltoche.fyde.resources.password_strength_very_strong
import com.peltoche.fyde.resources.password_strength_weak
import com.peltoche.fyde.resources.passwords_mismatch
import com.peltoche.fyde.resources.register_account_subtitle
import com.peltoche.fyde.resources.register_account_title
import com.peltoche.fyde.resources.register_acknowledge
import com.peltoche.fyde.resources.register_key_warning_body
import com.peltoche.fyde.resources.register_key_warning_lead
import com.peltoche.fyde.resources.register_password_warning
import com.peltoche.fyde.resources.register_submit
import com.peltoche.fyde.resources.register_submitting
import com.peltoche.fyde.resources.sign_up
import com.peltoche.fyde.resources.username_label
import com.peltoche.fyde.theme.AppColors
import com.peltoche.fyde.theme.AppDimensions
import com.peltoche.fyde.theme.AppIconSize
import com.peltoche.fyde.theme.AppSpacing
import com.peltoche.fyde.theme.AppTypography
import org.jetbrains.compose.resources.stringResource

/**
 * Registration: a username, a password and its confirmation. Pure
 * presentation — no `FydeSdk` import — so it can be redesigned without
 * touching auth wiring.
 *
 * At Compact it's the original single-column form, with a link to
 * [LoginScreen] under the button. At Medium and Expanded it's DO3
 * (`design/assets/screens/DO3-creer-le-coffre.png`): [VaultCreationBrandPanel]
 * beside "Your account", back to [WelcomeScreen] at the pane's top left, a
 * strength meter under the password, the key warning and an acknowledgement
 * to tick before Continue. Its errors (DO3b) show on blur or submit, not
 * while typing. The typed values live here, above both, so resizing the
 * window across Compact keeps them.
 *
 * A device without an OS credential store is handled by `App.kt`, which
 * covers this form with [InsecureStorageSheet] (O4, DO4b) on submit.
 */
@Composable
fun RegisterScreen(
    onRegister: (username: String, password: String) -> Unit,
    onNavigateToLogin: () -> Unit,
    onBack: () -> Unit,
    modifier: Modifier = Modifier,
    errorMessage: String? = null,
    isSubmitting: Boolean = false,
) {
    var username by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }
    var confirmPassword by remember { mutableStateOf("") }
    var acknowledged by remember { mutableStateOf(false) }
    val form = RegisterFormState(
        username = username,
        onUsernameChange = { username = it },
        password = password,
        onPasswordChange = { password = it },
        confirmPassword = confirmPassword,
        onConfirmPasswordChange = { confirmPassword = it },
        acknowledged = acknowledged,
        onAcknowledgedChange = { acknowledged = it },
        errorMessage = errorMessage,
        isSubmitting = isSubmitting,
        onRegister = onRegister,
    )

    when (LocalAppWindowSizeClass.current) {
        AppWindowSizeClass.Compact -> CompactRegister(form, onNavigateToLogin, modifier)
        AppWindowSizeClass.Medium, AppWindowSizeClass.Expanded -> VaultCreationSplitLayout(
            currentStep = VaultCreationStep.Account,
            modifier = modifier,
        ) { paneModifier ->
            WideRegisterPane(form, onBack, paneModifier)
        }
    }
}

/** What both layouts of [RegisterScreen] show and edit. */
private class RegisterFormState(
    val username: String,
    val onUsernameChange: (String) -> Unit,
    val password: String,
    val onPasswordChange: (String) -> Unit,
    val confirmPassword: String,
    val onConfirmPasswordChange: (String) -> Unit,
    val acknowledged: Boolean,
    val onAcknowledgedChange: (Boolean) -> Unit,
    val errorMessage: String?,
    val isSubmitting: Boolean,
    private val onRegister: (username: String, password: String) -> Unit,
) {
    val passwordsMatch: Boolean get() = password == confirmPassword

    /** Complete enough to submit; DO3 also asks for [acknowledged]. */
    fun canSubmit(requireAcknowledgement: Boolean): Boolean =
        !isSubmitting && username.isNotBlank() && password.isNotBlank() && passwordsMatch &&
            (acknowledged || !requireAcknowledgement)

    fun submit(requireAcknowledgement: Boolean) {
        if (canSubmit(requireAcknowledgement)) onRegister(username, password)
    }
}

/** The single-column form, with a link to [LoginScreen] under the button. */
@Composable
private fun CompactRegister(form: RegisterFormState, onNavigateToLogin: () -> Unit, modifier: Modifier) {
    val usernameFocus = remember { FocusRequester() }
    val submit = { form.submit(requireAcknowledgement = false) }

    // Land the cursor in the first field on arrival, so the form can be
    // filled and submitted from the keyboard alone.
    LaunchedEffect(Unit) { usernameFocus.requestFocus() }

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(AppColors.background)
            .safeContentPadding(),
        contentAlignment = Alignment.Center,
    ) {
        Column(
            modifier = Modifier
                .widthIn(max = AppDimensions.maxWidthDetail)
                .fillMaxWidth()
                .padding(horizontal = AppSpacing.space7),
            verticalArrangement = Arrangement.Center,
        ) {
            FydeLogo(modifier = Modifier.size(AppDimensions.authLogo))
            Spacer(Modifier.height(AppSpacing.space5))
            Text(
                text = stringResource(Res.string.create_account_title),
                style = AppTypography.title,
                color = AppColors.text,
            )
            Spacer(Modifier.height(AppSpacing.space3))
            // Said before the password is chosen, not after: fyde's
            // end-to-end encryption means no one, Fyde included, can reset
            // it, so a forgotten password loses every document for good.
            Text(
                text = stringResource(Res.string.register_password_warning),
                style = AppTypography.caption,
                color = AppColors.textMuted,
            )
            Spacer(Modifier.height(AppSpacing.space7))
            // Enter (or the soft keyboard's action key) moves on to the
            // next field, then submits from the last one — but only once
            // the form is complete, same as the button's `enabled` state.
            AppTextField(
                enabled = !form.isSubmitting,
                value = form.username,
                onValueChange = form.onUsernameChange,
                label = stringResource(Res.string.username_label),
                modifier = Modifier.focusRequester(usernameFocus),
                keyboardOptions = KeyboardOptions(imeAction = ImeAction.Next),
            )
            Spacer(Modifier.height(AppSpacing.space6))
            AppTextField(
                enabled = !form.isSubmitting,
                value = form.password,
                onValueChange = form.onPasswordChange,
                label = stringResource(Res.string.password_label),
                isPassword = true,
                keyboardOptions = KeyboardOptions(imeAction = ImeAction.Next),
            )
            Spacer(Modifier.height(AppSpacing.space6))
            AppTextField(
                enabled = !form.isSubmitting,
                value = form.confirmPassword,
                onValueChange = form.onConfirmPasswordChange,
                label = stringResource(Res.string.confirm_password_label),
                isPassword = true,
                keyboardOptions = KeyboardOptions(imeAction = ImeAction.Go),
                keyboardActions = KeyboardActions(onGo = { submit() }),
                // Attached to the field it actually describes — via the
                // same isError/errorText path AppTextField already exposes
                // for exactly this — rather than a separate caption below
                // the whole form with no accessible link to which field is
                // wrong.
                errorText = if (form.confirmPassword.isNotEmpty() && !form.passwordsMatch) {
                    stringResource(Res.string.passwords_mismatch)
                } else {
                    null
                },
            )
            if (form.errorMessage != null) {
                Spacer(Modifier.height(AppSpacing.space3))
                Text(
                    text = form.errorMessage,
                    style = AppTypography.caption,
                    color = AppColors.error,
                )
            }
            Spacer(Modifier.height(AppSpacing.space6))
            // AppButton, not a raw M3 Button: it carries the app's focus
            // ring and 48dp height, like every other primary action.
            AppButton(
                text = stringResource(Res.string.sign_up),
                onClick = submit,
                enabled = form.canSubmit(requireAcknowledgement = false),
                loading = form.isSubmitting,
                modifier = Modifier.fillMaxWidth(),
            )
            Spacer(Modifier.height(AppSpacing.space3))
            Row(
                horizontalArrangement = Arrangement.Center,
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.fillMaxWidth(),
            ) {
                Text(
                    text = stringResource(Res.string.has_account_prompt),
                    style = AppTypography.caption,
                    color = AppColors.textMuted,
                )
                AppTextButton(text = stringResource(Res.string.log_in), onClick = onNavigateToLogin, enabled = !form.isSubmitting)
            }
        }
    }
}

/**
 * DO3's form pane: back at the top left, hidden while the vault is created,
 * around the form centred in an [AppDimensions.authFormColumn] column.
 *
 * The mismatch error (DO3b) waits until the confirmation loses focus or
 * Enter is pressed on an incomplete form, then follows the typing. A
 * failed attempt shows the SDK's wording in an error note under the title,
 * as on DO2c. While submitting, the button spins with "Creating the
 * vault…" and an info note explains the key derivation, as on DO2b.
 */
@Composable
private fun WideRegisterPane(form: RegisterFormState, onBack: () -> Unit, modifier: Modifier) {
    val usernameFocus = remember { FocusRequester() }
    var confirmBlurred by remember { mutableStateOf(false) }
    var submitAttempted by remember { mutableStateOf(false) }
    val isDesktop = getPlatform().isDesktop
    val submit = {
        submitAttempted = true
        form.submit(requireAcknowledgement = true)
    }
    val showMismatch = (confirmBlurred || submitAttempted) && form.confirmPassword.isNotEmpty() && !form.passwordsMatch

    LaunchedEffect(Unit) { usernameFocus.requestFocus() }

    Box(modifier = modifier.background(AppColors.background).safeContentPadding()) {
        CenteredScrollColumn(
            modifier = Modifier.fillMaxSize(),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Column(
                modifier = Modifier
                    .widthIn(max = AppDimensions.authFormColumn)
                    .fillMaxWidth()
                    // Clears the back button above the form when the window is short.
                    .padding(horizontal = AppSpacing.space7, vertical = AppSpacing.space10 + AppSpacing.space8),
            ) {
                Text(
                    text = stringResource(Res.string.register_account_title),
                    style = AppTypography.display,
                    color = AppColors.text,
                )
                Spacer(Modifier.height(AppSpacing.space2))
                Text(
                    text = stringResource(Res.string.register_account_subtitle),
                    style = AppTypography.body,
                    color = AppColors.textMuted,
                )
                if (form.errorMessage != null) {
                    Spacer(Modifier.height(AppSpacing.space5))
                    AppNote(message = form.errorMessage, kind = AppNoteKind.Error, icon = { tint ->
                        AlertGlyph(modifier = Modifier.size(AppIconSize.sm), tint = tint)
                    })
                }
                Spacer(Modifier.height(AppSpacing.space6))
                // Enter moves on to the next field, then submits from the
                // last one — but only once the form is complete, same as
                // the button's `enabled` state.
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
                    isPassword = true,
                    keyboardOptions = KeyboardOptions(imeAction = ImeAction.Next),
                )
                if (form.password.isNotEmpty()) {
                    Spacer(Modifier.height(AppSpacing.space3))
                    PasswordStrengthMeter(passwordStrength(form.password), form.password.length)
                }
                Spacer(Modifier.height(AppSpacing.space5))
                AppTextField(
                    enabled = !form.isSubmitting,
                    value = form.confirmPassword,
                    onValueChange = form.onConfirmPasswordChange,
                    label = stringResource(Res.string.confirm_password_label),
                    isPassword = true,
                    revealable = false,
                    trailingIcon = if (form.confirmPassword.isNotEmpty() && form.passwordsMatch) {
                        { CheckGlyph(modifier = Modifier.size(AppIconSize.md)) }
                    } else {
                        null
                    },
                    errorText = if (showMismatch) stringResource(Res.string.passwords_mismatch) else null,
                    modifier = Modifier.onFocusChanged { if (!it.hasFocus && form.confirmPassword.isNotEmpty()) confirmBlurred = true },
                    keyboardOptions = KeyboardOptions(imeAction = ImeAction.Go),
                    keyboardActions = KeyboardActions(onGo = { submit() }),
                )
                Spacer(Modifier.height(AppSpacing.space5))
                // Said before the password is chosen, not after: fyde's
                // end-to-end encryption means no one, Fyde included, can
                // reset it, so a forgotten password loses every document.
                AppNote(
                    message = buildAnnotatedString {
                        withStyle(SpanStyle(fontWeight = FontWeight.SemiBold)) {
                            append(stringResource(Res.string.register_key_warning_lead))
                        }
                        append(' ')
                        append(stringResource(Res.string.register_key_warning_body))
                    },
                    kind = AppNoteKind.Warning,
                    icon = { tint -> KeyGlyph(modifier = Modifier.size(AppIconSize.md), tint = tint) },
                )
                Spacer(Modifier.height(AppSpacing.space4))
                AppCheckbox(
                    checked = form.acknowledged,
                    onCheckedChange = form.onAcknowledgedChange,
                    enabled = !form.isSubmitting,
                    label = stringResource(Res.string.register_acknowledge),
                    modifier = Modifier.fillMaxWidth(),
                )
                Spacer(Modifier.height(AppSpacing.space6))
                AppButton(
                    text = stringResource(Res.string.register_submit),
                    onClick = submit,
                    enabled = form.canSubmit(requireAcknowledgement = true),
                    loading = form.isSubmitting,
                    loadingText = stringResource(Res.string.register_submitting),
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
        }
    }
}

/** How hard a password is to guess, as O3's meter shows it: [bars] of four. */
internal enum class PasswordStrength(val bars: Int) { Weak(1), Fair(2), Strong(3), VeryStrong(4) }

/**
 * An advisory estimate by length alone — the server sets no rule, so a weak
 * password is warned about, never refused. Length, rather than character
 * classes, is what a passphrase of a few words gets right, and what the
 * weak hint ("add words or characters") asks for.
 */
internal fun passwordStrength(password: String): PasswordStrength = when {
    password.length < 10 -> PasswordStrength.Weak
    password.length < 14 -> PasswordStrength.Fair
    password.length < 20 -> PasswordStrength.Strong
    else -> PasswordStrength.VeryStrong
}

/** O3's meter under the password: four 4dp bars, lit in `warning` while weak or fair, `success` above, then the verdict. */
@Composable
internal fun PasswordStrengthMeter(strength: PasswordStrength, length: Int) {
    val lit = if (strength.bars <= PasswordStrength.Fair.bars) AppColors.warning else AppColors.success
    AppSegmentedMeter(segments = PasswordStrength.entries.size, lit = strength.bars, color = lit)
    Spacer(Modifier.height(AppSpacing.space2))
    Text(
        text = when (strength) {
            PasswordStrength.Weak -> stringResource(Res.string.password_strength_weak)
            PasswordStrength.Fair -> stringResource(Res.string.password_strength_fair, length)
            PasswordStrength.Strong -> stringResource(Res.string.password_strength_strong, length)
            PasswordStrength.VeryStrong -> stringResource(Res.string.password_strength_very_strong, length)
        },
        style = AppTypography.caption,
        color = AppColors.textMuted,
    )
}
