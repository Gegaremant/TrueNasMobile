package com.gegaremant.truenasmobile.ui.account

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import com.gegaremant.truenasmobile.R
import com.gegaremant.truenasmobile.data.helpers.MultiAccountPrefs
import com.gegaremant.truenasmobile.data.models.AccountProfile
import com.gegaremant.truenasmobile.data.models.LoginMethod
import com.gegaremant.truenasmobile.ui.components.ToastManager
import kotlinx.coroutines.launch

/**
 * Editor for a saved account: the login, the password and the server nickname.
 *
 * The password field stays empty on purpose - the stored one is not read back
 * into the form, and an empty field means "keep the current password". Typing a
 * new one overwrites it.
 */
@Composable
fun AccountEditDialog(
    profile: AccountProfile,
    onDismiss: () -> Unit,
    onSaved: () -> Unit
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()

    var nickname by remember { mutableStateOf(profile.server.nickname ?: "") }
    var username by remember { mutableStateOf(profile.account.username) }
    var password by remember { mutableStateOf("") }
    var isSaving by remember { mutableStateOf(false) }

    fun save() {
        if (isSaving) return
        if (username.isBlank()) {
            ToastManager.showErrorRes(R.string.account_edit_username_required)
            return
        }
        isSaving = true
        scope.launch {
            MultiAccountPrefs.saveServer(
                context,
                profile.server.copy(nickname = nickname.trim().ifBlank { null })
            )
            MultiAccountPrefs.saveAccount(
                context,
                profile.account.copy(username = username.trim())
            )
            if (password.isNotBlank()) {
                // API-key accounts have no password field; storing one for them
                // would only confuse the login path later.
                val method =
                    if (profile.account.loginMethod == LoginMethod.API_KEY) {
                        LoginMethod.PASSWORD
                    } else {
                        profile.account.loginMethod
                    }
                MultiAccountPrefs.saveAccountCredentials(
                    context = context,
                    accountId = profile.account.id,
                    loginMethod = method,
                    username = username.trim(),
                    password = password
                )
            }
            isSaving = false
            ToastManager.showSuccessRes(R.string.account_edit_saved)
            onSaved()
        }
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(stringResource(R.string.account_edit_title)) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                OutlinedTextField(
                    value = username,
                    onValueChange = { username = it },
                    label = { Text(stringResource(R.string.account_edit_username)) },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
                OutlinedTextField(
                    value = password,
                    onValueChange = { password = it },
                    label = { Text(stringResource(R.string.account_edit_password)) },
                    supportingText = { Text(stringResource(R.string.account_edit_password_hint)) },
                    singleLine = true,
                    visualTransformation = PasswordVisualTransformation(),
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password),
                    modifier = Modifier.fillMaxWidth()
                )
                OutlinedTextField(
                    value = nickname,
                    onValueChange = { nickname = it },
                    label = { Text(stringResource(R.string.account_edit_nickname)) },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
            }
        },
        confirmButton = {
            TextButton(onClick = { save() }, enabled = !isSaving) {
                if (isSaving) {
                    CircularProgressIndicator(
                        color = MaterialTheme.colorScheme.primary,
                        strokeWidth = 2.dp,
                        modifier = Modifier.size(16.dp)
                    )
                } else {
                    Text(stringResource(R.string.account_edit_save))
                }
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text(stringResource(R.string.common_cancel))
            }
        }
    )
}