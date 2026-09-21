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
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.peltoche.fyde.resources.Res
import com.peltoche.fyde.resources.confirm_password_label
import com.peltoche.fyde.resources.create_account_title
import com.peltoche.fyde.resources.has_account_prompt
import com.peltoche.fyde.resources.log_in
import com.peltoche.fyde.resources.password_label
import com.peltoche.fyde.resources.passwords_mismatch
import com.peltoche.fyde.resources.sign_up
import com.peltoche.fyde.resources.username_label
import org.jetbrains.compose.resources.stringResource

/**
 * Registration screen: username/password/confirm-password fields plus a
 * link back to [LoginScreen] for anyone who already has an account. Pure
 * presentation — no `FydeSdk` import — so it can be redesigned without
 * touching auth wiring.
 */
@Composable
fun RegisterScreen(
    onRegister: (username: String, password: String) -> Unit,
    onNavigateToLogin: () -> Unit,
    modifier: Modifier = Modifier,
    errorMessage: String? = null,
) {
    var username by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }
    var confirmPassword by remember { mutableStateOf("") }

    val passwordsMatch = password == confirmPassword
    val canSubmit = username.isNotBlank() && password.isNotBlank() && passwordsMatch

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .safeContentPadding(),
        contentAlignment = Alignment.Center,
    ) {
        Column(
            modifier = Modifier
                .widthIn(max = AuthFormMaxWidth)
                .fillMaxWidth()
                .padding(horizontal = 24.dp),
            verticalArrangement = Arrangement.Center,
        ) {
            Text(
                text = stringResource(Res.string.create_account_title),
                style = MaterialTheme.typography.headlineMedium,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onBackground,
            )
            Spacer(Modifier.height(24.dp))
            AuthTextField(value = username, onValueChange = { username = it }, label = stringResource(Res.string.username_label))
            Spacer(Modifier.height(12.dp))
            AuthTextField(
                value = password,
                onValueChange = { password = it },
                label = stringResource(Res.string.password_label),
                isPassword = true,
            )
            Spacer(Modifier.height(12.dp))
            AuthTextField(
                value = confirmPassword,
                onValueChange = { confirmPassword = it },
                label = stringResource(Res.string.confirm_password_label),
                isPassword = true,
            )
            if (confirmPassword.isNotEmpty() && !passwordsMatch) {
                Spacer(Modifier.height(8.dp))
                Text(
                    text = stringResource(Res.string.passwords_mismatch),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.error,
                )
            }
            if (errorMessage != null) {
                Spacer(Modifier.height(8.dp))
                Text(
                    text = errorMessage,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.error,
                )
            }
            Spacer(Modifier.height(20.dp))
            Button(
                onClick = { onRegister(username, password) },
                enabled = canSubmit,
                shape = AuthButtonShape,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(48.dp),
            ) {
                Text(stringResource(Res.string.sign_up))
            }
            Spacer(Modifier.height(8.dp))
            Row(
                horizontalArrangement = Arrangement.Center,
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.fillMaxWidth(),
            ) {
                Text(
                    text = stringResource(Res.string.has_account_prompt),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                TextButton(onClick = onNavigateToLogin) {
                    Text(stringResource(Res.string.log_in), fontWeight = FontWeight.SemiBold)
                }
            }
        }
    }
}
