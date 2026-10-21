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
import com.peltoche.fyde.components.form.AppCheckbox
import com.peltoche.fyde.components.form.AppTextField
import com.peltoche.fyde.resources.Res
import com.peltoche.fyde.resources.allow_insecure_storage_label
import com.peltoche.fyde.resources.login_create_vault
import com.peltoche.fyde.resources.login_no_vault
import com.peltoche.fyde.resources.login_password_help
import com.peltoche.fyde.resources.login_submit
import com.peltoche.fyde.resources.login_title
import com.peltoche.fyde.resources.password_label
import com.peltoche.fyde.resources.secure_storage_unavailable_login_warning
import com.peltoche.fyde.resources.username_label
import com.peltoche.fyde.theme.AppColors
import com.peltoche.fyde.theme.AppDimensions
import com.peltoche.fyde.theme.AppIconSize
import com.peltoche.fyde.theme.AppSpacing
import com.peltoche.fyde.theme.AppTypography
import org.jetbrains.compose.resources.stringResource

/**
 * Connexion (O2, `design/assets/screens/O2-connexion.png`): back to
 * [WelcomeScreen], "Open my vault", the username and password (with its
 * eye, and a line saying it never leaves the device), then Open the vault;
 * a link to [RegisterScreen] stays pinned at the bottom. Pure presentation
 * — no `FydeSdk` import — so it can be redesigned without touching auth
 * wiring.
 *
 * [noticeMessage] says why the user is here when it isn't by choice (the
 * session expired), as a warning note; [errorMessage] says why the last
 * attempt failed, as an error note. While [isSubmitting], the fields are
 * disabled and the button spins: key stretching takes seconds.
 *
 * When [isSecureStorageAvailable] is `false`, it explains the risk and
 * offers the same checkbox as [RegisterScreen] to keep the session's keys
 * in the local database instead; the form can only be submitted once it's
 * checked. Found via review: only registration offered it, so on such a
 * device an existing account could never log in — fyde-sdk refused, and
 * the form offered no way forward.
 */
@Composable
fun LoginScreen(
    onLogIn: (username: String, password: String, allowInsecureStorage: Boolean) -> Unit,
    onNavigateToRegister: () -> Unit,
    onBack: () -> Unit,
    modifier: Modifier = Modifier,
    noticeMessage: String? = null,
    errorMessage: String? = null,
    isSubmitting: Boolean = false,
    isSecureStorageAvailable: Boolean = true,
) {
    var username by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }
    var allowInsecureStorage by remember { mutableStateOf(false) }
    val usernameFocus = remember { FocusRequester() }

    val storageAccepted = isSecureStorageAvailable || allowInsecureStorage
    val canSubmit = !isSubmitting && username.isNotBlank() && password.isNotBlank() && storageAccepted
    val submit = { if (canSubmit) onLogIn(username, password, !isSecureStorageAvailable && allowInsecureStorage) }

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
                AppBackButton(onClick = onBack, enabled = !isSubmitting)
                Spacer(Modifier.height(AppSpacing.space4))
                Text(
                    text = stringResource(Res.string.login_title),
                    style = AppTypography.display,
                    color = AppColors.text,
                )
                if (noticeMessage != null) {
                    Spacer(Modifier.height(AppSpacing.space5))
                    AppNote(message = noticeMessage, kind = AppNoteKind.Warning, icon = { tint ->
                        ClockGlyph(modifier = Modifier.size(AppIconSize.md), tint = tint)
                    })
                }
                if (errorMessage != null) {
                    Spacer(Modifier.height(AppSpacing.space5))
                    AppNote(message = errorMessage, kind = AppNoteKind.Error, icon = { tint ->
                        AlertGlyph(modifier = Modifier.size(AppIconSize.sm), tint = tint)
                    })
                }
                Spacer(Modifier.height(AppSpacing.space6))
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
                Spacer(Modifier.height(AppSpacing.space5))
                AppTextField(
                    enabled = !isSubmitting,
                    value = password,
                    onValueChange = { password = it },
                    label = stringResource(Res.string.password_label),
                    supportingText = stringResource(Res.string.login_password_help),
                    isPassword = true,
                    keyboardOptions = KeyboardOptions(imeAction = ImeAction.Go),
                    keyboardActions = KeyboardActions(onGo = { submit() }),
                )
                if (!isSecureStorageAvailable) {
                    Spacer(Modifier.height(AppSpacing.space4))
                    Text(
                        text = stringResource(Res.string.secure_storage_unavailable_login_warning),
                        style = AppTypography.caption,
                        color = AppColors.textMuted,
                    )
                    Spacer(Modifier.height(AppSpacing.space2))
                    AppCheckbox(
                        checked = allowInsecureStorage,
                        onCheckedChange = { allowInsecureStorage = it },
                        enabled = !isSubmitting,
                        label = stringResource(Res.string.allow_insecure_storage_label),
                    )
                }
                Spacer(Modifier.height(AppSpacing.space6))
                AppButton(
                    text = stringResource(Res.string.login_submit),
                    onClick = submit,
                    enabled = canSubmit,
                    loading = isSubmitting,
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
                Text(
                    text = stringResource(Res.string.login_no_vault),
                    style = AppTypography.body,
                    color = AppColors.textMuted,
                )
                AppTextButton(
                    text = stringResource(Res.string.login_create_vault),
                    onClick = onNavigateToRegister,
                    enabled = !isSubmitting,
                )
            }
        }
    }
}
