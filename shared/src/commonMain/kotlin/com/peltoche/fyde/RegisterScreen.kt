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
import androidx.compose.ui.text.input.ImeAction
import com.peltoche.fyde.resources.Res
import com.peltoche.fyde.resources.confirm_password_label
import com.peltoche.fyde.resources.create_account_title
import com.peltoche.fyde.resources.has_account_prompt
import com.peltoche.fyde.resources.log_in
import com.peltoche.fyde.resources.password_label
import com.peltoche.fyde.resources.passwords_mismatch
import com.peltoche.fyde.resources.register_password_warning
import com.peltoche.fyde.resources.sign_up
import com.peltoche.fyde.resources.username_label
import com.peltoche.fyde.components.form.AppTextField
import com.peltoche.fyde.components.action.AppButton
import com.peltoche.fyde.components.action.AppTextButton
import com.peltoche.fyde.theme.AppColors
import com.peltoche.fyde.theme.AppDimensions
import com.peltoche.fyde.theme.AppSpacing
import com.peltoche.fyde.theme.AppTypography
import org.jetbrains.compose.resources.stringResource

/**
 * Registration screen: username/password/confirm-password fields plus a
 * link back to [LoginScreen] for anyone who already has an account. Pure
 * presentation — no `FydeSdk` import — so it can be redesigned without
 * touching auth wiring.
 *
 * A device without an OS credential store is handled by `App.kt`, which
 * covers this form with [InsecureStorageSheet] (O4) on submit.
 */
@Composable
fun RegisterScreen(
    onRegister: (username: String, password: String) -> Unit,
    onNavigateToLogin: () -> Unit,
    modifier: Modifier = Modifier,
    errorMessage: String? = null,
    isSubmitting: Boolean = false,
) {
    var username by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }
    val usernameFocus = remember { FocusRequester() }
    var confirmPassword by remember { mutableStateOf("") }

    val passwordsMatch = password == confirmPassword
    val canSubmit = !isSubmitting && username.isNotBlank() && password.isNotBlank() && passwordsMatch
    val submit = { if (canSubmit) onRegister(username, password) }

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
                enabled = !isSubmitting,
                value = username,
                onValueChange = { username = it },
                label = stringResource(Res.string.username_label),
                modifier = Modifier.focusRequester(usernameFocus),
                keyboardOptions = KeyboardOptions(imeAction = ImeAction.Next),
            )
            Spacer(Modifier.height(AppSpacing.space6))
            AppTextField(
                enabled = !isSubmitting,
                value = password,
                onValueChange = { password = it },
                label = stringResource(Res.string.password_label),
                isPassword = true,
                keyboardOptions = KeyboardOptions(imeAction = ImeAction.Next),
            )
            Spacer(Modifier.height(AppSpacing.space6))
            AppTextField(
                enabled = !isSubmitting,
                value = confirmPassword,
                onValueChange = { confirmPassword = it },
                label = stringResource(Res.string.confirm_password_label),
                isPassword = true,
                keyboardOptions = KeyboardOptions(imeAction = ImeAction.Go),
                keyboardActions = KeyboardActions(onGo = { submit() }),
                // Attached to the field it actually describes — via the
                // same isError/errorText path AppTextField already exposes
                // for exactly this — rather than a separate caption below
                // the whole form with no accessible link to which field is
                // wrong.
                errorText = if (confirmPassword.isNotEmpty() && !passwordsMatch) {
                    stringResource(Res.string.passwords_mismatch)
                } else {
                    null
                },
            )
            if (errorMessage != null) {
                Spacer(Modifier.height(AppSpacing.space3))
                Text(
                    text = errorMessage,
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
                enabled = canSubmit,
                loading = isSubmitting,
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
                AppTextButton(text = stringResource(Res.string.log_in), onClick = onNavigateToLogin, enabled = !isSubmitting)
            }
        }
    }
}
