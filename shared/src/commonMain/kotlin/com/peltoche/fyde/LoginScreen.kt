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
import androidx.compose.foundation.layout.widthIn
import androidx.compose.material3.Button
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import com.peltoche.fyde.resources.Res
import com.peltoche.fyde.resources.log_in
import com.peltoche.fyde.resources.no_account_prompt
import com.peltoche.fyde.resources.password_label
import com.peltoche.fyde.resources.sign_up
import com.peltoche.fyde.resources.username_label
import com.peltoche.fyde.components.form.AppTextField
import com.peltoche.fyde.theme.AppColors
import com.peltoche.fyde.theme.AppDimensions
import com.peltoche.fyde.theme.AppShapes
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
) {
    var username by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }

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
            Text(
                text = stringResource(Res.string.log_in),
                style = AppTypography.title,
                color = AppColors.text,
            )
            Spacer(Modifier.height(AppSpacing.space7))
            AppTextField(value = username, onValueChange = { username = it }, label = stringResource(Res.string.username_label))
            Spacer(Modifier.height(AppSpacing.space4))
            AppTextField(
                value = password,
                onValueChange = { password = it },
                label = stringResource(Res.string.password_label),
                isPassword = true,
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
            Button(
                onClick = { onLogIn(username, password) },
                enabled = username.isNotBlank() && password.isNotBlank(),
                shape = AppShapes.button,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(AppDimensions.touchTargetMin),
            ) {
                Text(stringResource(Res.string.log_in))
            }
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
                TextButton(onClick = onNavigateToRegister) {
                    Text(stringResource(Res.string.sign_up))
                }
            }
        }
    }
}
