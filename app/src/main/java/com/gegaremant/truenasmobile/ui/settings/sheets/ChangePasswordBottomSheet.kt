package com.gegaremant.truenasmobile.ui.settings.sheets

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Circle
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Security
import androidx.compose.material.icons.filled.VpnKey
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import androidx.compose.ui.res.stringResource
import com.gegaremant.truenasmobile.R
import com.gegaremant.truenasmobile.data.api.TrueNASApiManager
import com.gegaremant.truenasmobile.ui.components.UnifiedScreenHeader

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ChangePasswordScreen(
    manager: TrueNASApiManager,
    onSubmit: (oldPassword: String, newPassword: String) -> Unit,
    onNavigateBack: () -> Unit,
) {
    var oldPassword by remember { mutableStateOf("") }
    var newPassword by remember { mutableStateOf("") }
    var confirmPassword by remember { mutableStateOf("") }

    val oldPasswordFocusRequester = remember { FocusRequester() }
    val newPasswordFocusRequester = remember { FocusRequester() }
    val confirmPasswordFocusRequester = remember { FocusRequester() }

    val keyboardController = LocalSoftwareKeyboardController.current

    val passwordsMatch = newPassword == confirmPassword
    val isFormValid = oldPassword.isNotEmpty() &&
    newPassword.isNotEmpty() &&
    confirmPassword.isNotEmpty() &&
    passwordsMatch &&
    newPassword.length >= 8

    Column(
        modifier = Modifier
            .fillMaxSize()
            .statusBarsPadding()
    ) {
        UnifiedScreenHeader(
        title = stringResource(R.string.settings_change_password_title),
        subtitle = stringResource(R.string.settings_change_password_subtitle),
        isLoading = false,
        isRefreshing = false,
        error = null,
        onDismissError = {},
        manager = manager,
        onBackPressed = onNavigateBack
    )

        PasswordChangeForm(
            modifier = Modifier.weight(1f),
            oldPassword = oldPassword,
            newPassword = newPassword,
            confirmPassword = confirmPassword,
            passwordsMatch = passwordsMatch,
            isFormValid = isFormValid,
            onOldPasswordChange = { oldPassword = it },
            onNewPasswordChange = { newPassword = it },
            onConfirmPasswordChange = { confirmPassword = it },
            oldPasswordFocusRequester = oldPasswordFocusRequester,
            newPasswordFocusRequester = newPasswordFocusRequester,
            confirmPasswordFocusRequester = confirmPasswordFocusRequester,
            onSubmit = {
                keyboardController?.hide()
                onSubmit(oldPassword, newPassword)
            },
            onCancel = {
                keyboardController?.hide()
            }
        )
    }

}

@Composable
private fun PasswordChangeForm(
    modifier: Modifier = Modifier,
    oldPassword: String,
    newPassword: String,
    confirmPassword: String,
    passwordsMatch: Boolean,
    isFormValid: Boolean,
    onOldPasswordChange: (String) -> Unit,
    onNewPasswordChange: (String) -> Unit,
    onConfirmPasswordChange: (String) -> Unit,
    oldPasswordFocusRequester: FocusRequester,
    newPasswordFocusRequester: FocusRequester,
    confirmPasswordFocusRequester: FocusRequester,
    onSubmit: () -> Unit,
    onCancel: () -> Unit
) {
    Column(
        modifier = modifier
            .fillMaxWidth()
            .verticalScroll(rememberScrollState())
            .padding(24.dp),
        verticalArrangement = Arrangement.spacedBy(24.dp)
    ) {
        PasswordInfoSection(
            title = stringResource(R.string.settings_security_requirements),
            icon = Icons.Default.Info
        ) {
            PasswordRequirementRow(
                text = stringResource(R.string.settings_password_min_length),
                met = newPassword.length >= 8
            )
            PasswordRequirementRow(
                text = stringResource(R.string.settings_passwords_match_req),
                met = passwordsMatch && confirmPassword.isNotEmpty()
            )
        }

        PasswordInfoSection(
            title = stringResource(R.string.settings_password_details),
            icon = Icons.Default.Lock
        ) {
            OutlinedTextField(
                value = oldPassword,
                onValueChange = onOldPasswordChange,
                label = { Text(stringResource(R.string.settings_current_password)) },
                modifier = Modifier
                    .fillMaxWidth()
                    .focusRequester(oldPasswordFocusRequester),
                visualTransformation = PasswordVisualTransformation(),
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password),
                trailingIcon = {
                    Icon(Icons.Default.VpnKey, contentDescription = stringResource(R.string.settings_current_password_cd))
                },
                singleLine = true
            )

            Spacer(modifier = Modifier.height(16.dp))

            OutlinedTextField(
                value = newPassword,
                onValueChange = onNewPasswordChange,
                label = { Text(stringResource(R.string.settings_new_password)) },
                modifier = Modifier
                    .fillMaxWidth()
                    .focusRequester(newPasswordFocusRequester),
                visualTransformation = PasswordVisualTransformation(),
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password),
                trailingIcon = {
                    Icon(Icons.Default.Lock, contentDescription = stringResource(R.string.settings_new_password_cd))
                },
                singleLine = true,
                isError = newPassword.isNotEmpty() && newPassword.length < 8
            )

            Spacer(modifier = Modifier.height(16.dp))

            OutlinedTextField(
                value = confirmPassword,
                onValueChange = onConfirmPasswordChange,
                label = { Text(stringResource(R.string.settings_confirm_new_password)) },
                modifier = Modifier
                    .fillMaxWidth()
                    .focusRequester(confirmPasswordFocusRequester),
                visualTransformation = PasswordVisualTransformation(),
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password),
                trailingIcon = {
                    Icon(
                        imageVector = if (passwordsMatch && confirmPassword.isNotEmpty()) {
                            Icons.Default.CheckCircle
                        } else {
                            Icons.Default.Lock
                        },
                        contentDescription = stringResource(R.string.settings_confirm_password_cd),
                        tint = if (passwordsMatch && confirmPassword.isNotEmpty()) {
                            MaterialTheme.colorScheme.primary
                        } else {
                            MaterialTheme.colorScheme.onSurfaceVariant
                        }
                    )
                },
                singleLine = true,
                isError = confirmPassword.isNotEmpty() && !passwordsMatch
            )

            if (confirmPassword.isNotEmpty() && !passwordsMatch) {
                Text(
                    text = stringResource(R.string.settings_passwords_do_not_match),
                    color = MaterialTheme.colorScheme.error,
                    style = MaterialTheme.typography.bodySmall,
                    modifier = Modifier.padding(top = 4.dp, start = 4.dp)
                )
            }
        }

        Column(
            modifier = Modifier
                .height(120.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Button(
                onClick = onSubmit,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(56.dp),
                enabled = isFormValid,
                shape = RoundedCornerShape(12.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.Security,
                    contentDescription = null,
                    modifier = Modifier.size(20.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = stringResource(R.string.settings_change_password_button),
                    style = MaterialTheme.typography.labelLarge,
                    fontWeight = FontWeight.SemiBold
                )
            }

            OutlinedButton(
                onClick = onCancel,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(56.dp),
                shape = RoundedCornerShape(12.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.Close,
                    contentDescription = null,
                    modifier = Modifier.size(20.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = stringResource(R.string.common_cancel),
                    style = MaterialTheme.typography.labelLarge,
                    fontWeight = FontWeight.SemiBold
                )
            }
        }

        Spacer(modifier = Modifier.height(16.dp))
    }
}

@Composable
private fun PasswordInfoSection(
    title: String,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    content: @Composable ColumnScope.() -> Unit
) {
    Column {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.padding(bottom = 16.dp)
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.primary,
                modifier = Modifier.size(24.dp)
            )
            Spacer(modifier = Modifier.width(8.dp))
            Text(
                text = title,
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurface
            )
        }
        Column(content = content)
    }
}

@Composable
private fun PasswordRequirementRow(
    text: String,
    met: Boolean
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 6.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(
            imageVector = if (met) Icons.Default.CheckCircle else Icons.Default.Circle,
            contentDescription = null,
            tint = if (met) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outline,
            modifier = Modifier.size(16.dp)
        )
        Spacer(modifier = Modifier.width(12.dp))
        Text(
            text = text,
            style = MaterialTheme.typography.bodyMedium,
            color = if (met) {
                MaterialTheme.colorScheme.onSurface
            } else {
                MaterialTheme.colorScheme.onSurfaceVariant
            }
        )
    }
}