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
import com.peltoche.fyde.resources.log_in
import com.peltoche.fyde.resources.no_account_prompt
import com.peltoche.fyde.resources.password_label
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
 * Login screen: username/password fields plus a link to [RegisterScreen]
 * for anyone who doesn't have an account yet. Pure presentation — no
 * `FydeSdk` import — so it can be redesigned without touching auth wiring.
 */
@Composable
fun LoginScreen(
    onLogIn: (username: String, password: String) -> Unit,
    onNavigateToRegister: () -> Unit,
    modifier: Modifier = Modifier,
    errorMessage: String? = null,
    isSubmitting: Boolean = false,
) {
    var username by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }
    val usernameFocus = remember { FocusRequester() }

    val canSubmit = !isSubmitting && username.isNotBlank() && password.isNotBlank()
    val submit = { if (canSubmit) onLogIn(username, password) }

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
                text = stringResource(Res.string.log_in),
                style = AppTypography.title,
                color = AppColors.text,
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
            Spacer(Modifier.height(AppSpacing.space4))
            AppTextField(
                enabled = !isSubmitting,
                value = password,
                onValueChange = { password = it },
                label = stringResource(Res.string.password_label),
                isPassword = true,
                keyboardOptions = KeyboardOptions(imeAction = ImeAction.Go),
                keyboardActions = KeyboardActions(onGo = { submit() }),
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
                text = stringResource(Res.string.log_in),
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
                    text = stringResource(Res.string.no_account_prompt),
                    style = AppTypography.caption,
                    color = AppColors.textMuted,
                )
                AppTextButton(text = stringResource(Res.string.sign_up), onClick = onNavigateToRegister, enabled = !isSubmitting)
            }
        }
    }
}
