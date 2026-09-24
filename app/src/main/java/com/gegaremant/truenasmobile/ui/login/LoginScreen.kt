package com.gegaremant.truenasmobile.ui.login

import android.app.Application
import android.util.Log
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Key
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Security
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.material.icons.filled.Web
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DividerDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavController
import com.gegaremant.truenasmobile.R
import com.gegaremant.truenasmobile.data.TrueNASClient
import com.gegaremant.truenasmobile.data.api.TrueNASApiManager
import com.gegaremant.truenasmobile.data.helpers.Prefs
import com.gegaremant.truenasmobile.data.models.Auth.LoginMode
import com.gegaremant.truenasmobile.data.models.Config
import com.gegaremant.truenasmobile.ui.background.WavyGradientBackground
import com.gegaremant.truenasmobile.ui.components.ToastManager
import com.gegaremant.truenasmobile.ui.setup.ServerConfigBottomSheet
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LoginScreen(
    existingManager: TrueNASApiManager?,
    navController: NavController,
    onManagerInitialized: (TrueNASApiManager) -> Unit,
    onLoginSuccess : ()->Unit,
    startInOtpMode: Boolean = false,
    totpUsername: String? = null
) {
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current
    val application = context.applicationContext as Application
    val (savedUrl, savedInsecure) = remember { Prefs.load(context) }

    // Local state for manager - use existing or create new
    var localManager by remember(existingManager) {
        mutableStateOf(existingManager)
    }
    var showSetupSheet by remember { mutableStateOf( savedUrl == null) }

    val viewModel: LoginScreenViewModel = viewModel(
        factory = LoginViewModelFactory(existingManager, application)
    )
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()

    // Enter OTP mode on cold start for TOTP users
    LaunchedEffect(startInOtpMode, totpUsername) {
        if (startInOtpMode && totpUsername != null) {
            viewModel.enterOtpMode(totpUsername)
        }
    }

    // Handle manager initialization when URL is configured but manager is null
    LaunchedEffect(savedUrl, savedInsecure, localManager) {
        if (localManager == null && savedUrl != null) {
            lifecycleOwner.lifecycleScope.launch {
                try {
                    ToastManager.showInfo(context.getString(R.string.common_connecting_to_server))

                    val config = Config.ClientConfig(
                        serverUrl = savedUrl,
                        insecure = savedInsecure,
                        connectionTimeoutMs = 15000,
                        enablePing = true,
                        enableDebugLogging = false
                    )

                    val client = TrueNASClient(config)
                    val newManager = TrueNASApiManager(client,context.applicationContext)
                    val connected = newManager.connect()

                    if (connected) {
                        localManager = newManager
                        onManagerInitialized(newManager)
                        viewModel.updateManager(newManager)
                        ToastManager.showSuccess(context.getString(R.string.login_connected_success))
                        showSetupSheet = false
                    } else {
                        ToastManager.showError(context.getString(R.string.login_initial_connection_failed))
                        showSetupSheet = true
                    }
                } catch (e: Exception) {
                    ToastManager.showError(context.getString(R.string.login_connection_failed_pattern, e.message ?: ""))
                    showSetupSheet = true
                }
            }
        }
    }

    // Handle login success
    LaunchedEffect(uiState.isLoginSuccessful) {
        if (uiState.isLoginSuccessful) {
            Log.d("LoginScreen", "Login successful, invoking onLoginSuccess callback.")

            onLoginSuccess()

            viewModel.handleEvent(LoginEvent.LoginNavigationCompleted)
        }
    }

    Box(modifier = Modifier.fillMaxSize()) {
        when {
            // OTP takes over the entire screen — no login form visible
            localManager != null && uiState.showOtpField -> {
                OtpFullScreen(
                    viewModel = viewModel,
                    uiState = uiState,
                    onChangeServerConfig = { showSetupSheet = true }
                )
            }
            localManager != null -> {
                LoginContent(
                    manager = localManager!!,
                    viewModel = viewModel,
                    uiState = uiState,
                    onChangeServerConfig = { showSetupSheet = true },
                    apiKey = uiState.apiKey,
                    onApiKeyChange = { newApikey -> viewModel.handleEvent(LoginEvent.UpdateApiKey(newApikey)) },
                    isApiKeyVisible = uiState.isApiKeyVisible,
                    onToggleVisibilityClick = { viewModel.handleEvent(LoginEvent.ToggleApiKeyVisibility) },
                    saveForAutoLogin = uiState.saveDetailsForAutoLogin,
                    onSaveForAutoLoginChange = {change -> viewModel.handleEvent(LoginEvent.UpdateSaveApiKey(change,application)) },
                    isLoading = uiState.isLoading,
                )
            }
            savedUrl != null -> {
                Column(
                    modifier = Modifier.fillMaxSize(),
                    verticalArrangement = Arrangement.Center,
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    CircularProgressIndicator()
                    Spacer(modifier = Modifier.height(16.dp))
                    Text(stringResource(R.string.common_connecting_to_server))
                    Spacer(modifier = Modifier.height(16.dp))
                    Button(onClick = { showSetupSheet = true }) {
                        Text(stringResource(R.string.login_configure_server))
                    }
                }
            }
            else -> {
                ServerConfigurationPrompt(
                    onConfigureClick = { showSetupSheet = true }
                )
            }
        }
    }
        if (showSetupSheet) {
            ServerConfigBottomSheet(
                onDismiss = {
                    showSetupSheet = false
                },
                onConfigured = { url, insecure ->
                    lifecycleOwner.lifecycleScope.launch {
                        try {
                            ToastManager.showInfo(context.getString(R.string.common_connecting_to_server))

                            val config = Config.ClientConfig(
                                serverUrl = url,
                                insecure = insecure,
                                connectionTimeoutMs = 15000,
                                enablePing = false,
                                enableDebugLogging = false
                            )

                            val client = TrueNASClient(config)
                            val newManager = TrueNASApiManager(client,context.applicationContext)
                            val connected = newManager.connect()

                            if (connected) {
                                Prefs.save(context, url, insecure)


                                localManager = newManager
                                onManagerInitialized(newManager)
                                viewModel.updateManager(newManager)

                                showSetupSheet = false
                                ToastManager.showSuccess(context.getString(R.string.login_connected_successfully))
                            } else {
                                ToastManager.showError(context.getString(R.string.login_failed_connect_details))
                                showSetupSheet = true
                            }

                        } catch (e: Exception) {
                            ToastManager.showError(context.getString(R.string.login_failed_connect_pattern, e.message ?: ""))
                        }
                    }
                },
                initialUrl = savedUrl,
                initialInsecure = savedInsecure,
                showChangeUrlOption = savedUrl != null
            )
        }
    }


@Composable
private fun LoginContent(
    manager: TrueNASApiManager,
    viewModel: LoginScreenViewModel,
    uiState: LoginUiState,
    onChangeServerConfig: () -> Unit,
    apiKey: String,
    onApiKeyChange: (String) -> Unit,
    isApiKeyVisible: Boolean,
    onToggleVisibilityClick: () -> Unit,
    saveForAutoLogin: Boolean,
    onSaveForAutoLoginChange: (Boolean) -> Unit,
    isLoading: Boolean
) {
    val context = LocalContext.current

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(
                brush = Brush.verticalGradient(
                    colors = listOf(
                        MaterialTheme.colorScheme.surface,
                        MaterialTheme.colorScheme.surfaceContainer
                    )
                )
            )
    ) {
        WavyGradientBackground {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .verticalScroll(rememberScrollState())
                    .padding(24.dp),
                verticalArrangement = Arrangement.Center,
                horizontalAlignment = Alignment.Start
            ) {
                // Header Section
                Column(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalAlignment = Alignment.Start
                ) {
                    Text(
                        text = buildAnnotatedString {
                            append(stringResource(R.string.login_enter_your))
                            withStyle(style = SpanStyle(color = MaterialTheme.colorScheme.primary)) {
                                append(stringResource(R.string.login_credentials))
                            }
                        },
                        fontSize = 32.sp,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface,
                        lineHeight = 38.sp,
                        modifier = Modifier.padding(bottom = 8.dp)
                    )

                    Text(
                        text = stringResource(R.string.login_sign_in_to_continue),
                        fontSize = 16.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(bottom = 32.dp)
                    )
                }
                ConnectionStatusCard(uiState.connectionStatus) {
                    viewModel.handleEvent(LoginEvent.CheckConnection)
                }
                Spacer(modifier = Modifier.height(16.dp))
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(bottom = 24.dp),
                    horizontalArrangement = Arrangement.Start
                ) {
                    LoginMethodTab(
                        text = stringResource(R.string.login_mode_password),
                        isSelected = uiState.loginMode == LoginMode.PASSWORD,
                        onClick = {
                            viewModel.handleEvent(LoginEvent.UpdateLoginMode(LoginMode.PASSWORD))
                        }
                    )

                    Spacer(modifier = Modifier.width(24.dp))

                    LoginMethodTab(
                        text = stringResource(R.string.login_mode_api_key),
                        isSelected = uiState.loginMode == LoginMode.API_KEY,
                        onClick = {
                            viewModel.handleEvent(LoginEvent.UpdateLoginMode(LoginMode.API_KEY))
                        }
                    )
                }
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    elevation = CardDefaults.cardElevation(defaultElevation = 4.dp),
                    shape = RoundedCornerShape(16.dp)
                ) {
                    Column(
                        modifier = Modifier.padding(20.dp),
                        verticalArrangement = Arrangement.spacedBy(16.dp)
                    ) {
                        when (uiState.loginMode) {
                            LoginMode.PASSWORD -> {
                                OutlinedTextField(
                                    value = uiState.username,
                                    onValueChange = {
                                        viewModel.handleEvent(LoginEvent.UpdateUsername(it))
                                    },
                                    label = { Text(stringResource(R.string.login_username_hint)) },
                                    leadingIcon = {
                                        Icon(
                                            imageVector = Icons.Default.Person,
                                            contentDescription = stringResource(R.string.login_username_hint)
                                        )
                                    },
                                    singleLine = true,
                                    keyboardOptions = KeyboardOptions(imeAction = ImeAction.Next),
                                    modifier = Modifier.fillMaxWidth(),
                                    shape = RoundedCornerShape(12.dp),
                                    colors = OutlinedTextFieldDefaults.colors(
                                        focusedBorderColor = MaterialTheme.colorScheme.primary,
                                        unfocusedBorderColor = MaterialTheme.colorScheme.outline.copy(alpha = 0.5f)
                                    ),
                                    enabled = !uiState.isLoading
                                )
                                OutlinedTextField(
                                    value = uiState.password,
                                    onValueChange = {
                                        viewModel.handleEvent(LoginEvent.UpdatePassword(it))
                                    },
                                    label = { Text(stringResource(R.string.login_password_hint)) },
                                    leadingIcon = {
                                        Icon(
                                            imageVector = Icons.Default.Lock,
                                            contentDescription = stringResource(R.string.login_password_hint)
                                        )
                                    },
                                    trailingIcon = {
                                        IconButton(
                                            onClick = {
                                                viewModel.handleEvent(LoginEvent.TogglePasswordVisibility)
                                            }
                                        ) {
                                            Icon(
                                                imageVector = if (uiState.isPasswordVisible) {
                                                    Icons.Default.VisibilityOff
                                                } else {
                                                    Icons.Default.Visibility
                                                },
                                                contentDescription = if (uiState.isPasswordVisible) {
                                                    stringResource(R.string.login_hide_password)
                                                } else {
                                                    stringResource(R.string.login_show_password)
                                                }
                                            )
                                        }
                                    },
                                    visualTransformation = if (uiState.isPasswordVisible) {
                                        VisualTransformation.None
                                    } else {
                                        PasswordVisualTransformation()
                                    },
                                    singleLine = true,
                                    keyboardOptions = KeyboardOptions(
                                        keyboardType = KeyboardType.Password,
                                        imeAction = ImeAction.Done
                                    ),
                                    keyboardActions = KeyboardActions(
                                        onDone = {
                                            viewModel.handleEvent(LoginEvent.Login(context))
                                        }
                                    ),
                                    modifier = Modifier.fillMaxWidth(),
                                    shape = RoundedCornerShape(12.dp),
                                    colors = OutlinedTextFieldDefaults.colors(
                                        focusedBorderColor = MaterialTheme.colorScheme.primary,
                                        unfocusedBorderColor = MaterialTheme.colorScheme.outline.copy(alpha = 0.5f)
                                    ),
                                    enabled = !uiState.isLoading
                                )
                                Spacer(modifier = Modifier.height(2.dp))
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text(
                                        text = stringResource(R.string.login_save_details_autologin),
                                        style = MaterialTheme.typography.bodyMedium,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                                        modifier = Modifier.weight(1f)
                                    )
                                    Spacer(modifier = Modifier.width(16.dp))
                                    Switch(
                                        checked = saveForAutoLogin,
                                        onCheckedChange = onSaveForAutoLoginChange,
                                        enabled = !isLoading
                                    )
                                }
                            }

                            LoginMode.API_KEY -> {
                                OutlinedTextField(
                                    value = apiKey,
                                    onValueChange = onApiKeyChange,
                                    label = { Text(stringResource(R.string.login_paste_api_key)) },
                                    leadingIcon = {
                                        Icon(
                                            imageVector = Icons.Default.Key,
                                            contentDescription = stringResource(R.string.common_api_key)
                                        )
                                    },
                                    trailingIcon = {
                                        IconButton(onClick = onToggleVisibilityClick) {
                                            Icon(
                                                imageVector = if (isApiKeyVisible) Icons.Filled.VisibilityOff else Icons.Filled.Visibility,
                                                contentDescription = if (isApiKeyVisible) stringResource(R.string.login_hide_api_key) else stringResource(R.string.login_show_api_key)
                                            )
                                        }
                                    },
                                    visualTransformation = if (isApiKeyVisible) VisualTransformation.None else PasswordVisualTransformation(),
                                    modifier = Modifier.fillMaxWidth(),
                                    shape = RoundedCornerShape(12.dp),
                                    colors = OutlinedTextFieldDefaults.colors(
                                        focusedBorderColor = MaterialTheme.colorScheme.primary,
                                        unfocusedBorderColor = MaterialTheme.colorScheme.outline.copy(alpha = 0.5f)
                                    ),
                                    singleLine = true,
                                    keyboardOptions = KeyboardOptions(
                                        keyboardType = KeyboardType.Password,
                                        imeAction = ImeAction.Done
                                    ),
                                    keyboardActions = KeyboardActions(
                                        onDone = {
                                            viewModel.handleEvent(LoginEvent.Login(context))
                                        }
                                    ),
                                    enabled = !isLoading
                                )
                                Spacer(modifier = Modifier.height(2.dp))
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text(
                                        text = stringResource(R.string.login_save_key_autologin),
                                        style = MaterialTheme.typography.bodyMedium,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                                        modifier = Modifier.weight(1f)
                                    )
                                    Spacer(modifier = Modifier.width(16.dp))
                                    Switch(
                                        checked = saveForAutoLogin,
                                        onCheckedChange = onSaveForAutoLoginChange,
                                        enabled = !isLoading
                                    )
                                }
                            }
                        }
                    }
                }

                // ── Only shown when OTP is NOT active ──
                Spacer(modifier = Modifier.height(32.dp))
                Button(
                    onClick = {
                        viewModel.handleEvent(LoginEvent.Login(context))
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(56.dp),
                    shape = RoundedCornerShape(12.dp),
                    enabled = !uiState.isLoading && uiState.connectionStatus is ConnectionStatus.Connected
                ) {
                    if (uiState.isLoading) {
                        Row(
                            horizontalArrangement = Arrangement.Center,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            CircularProgressIndicator(
                                modifier = Modifier.size(20.dp),
                                strokeWidth = 2.dp,
                                color = MaterialTheme.colorScheme.onPrimary
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(stringResource(R.string.login_signing_in))
                        }
                    } else {
                        Text(
                            stringResource(R.string.login_sign_in),
                            fontSize = 16.sp,
                            fontWeight = FontWeight.Medium
                        )
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))
                Text(
                    text = stringResource(
                        R.string.login_need_help,
                        if (uiState.loginMode == LoginMode.PASSWORD) {
                            stringResource(R.string.login_need_help_account)
                        } else {
                            stringResource(R.string.login_need_help_api_key)
                        }
                    ),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.primary,
                    textAlign = TextAlign.Center,
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable {
                            ToastManager.showInfo(context.getString(R.string.login_contact_administrator))
                        }
                )

                Spacer(modifier = Modifier.height(30.dp))
                ServerInfoSection(
                    onChangeServerClick = onChangeServerConfig
                )
            }
        }
    }
}


@Composable
private fun OtpFullScreen(
    viewModel: LoginScreenViewModel,
    uiState: LoginUiState,
    onChangeServerConfig: () -> Unit
) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(
                brush = Brush.verticalGradient(
                    colors = listOf(
                        MaterialTheme.colorScheme.surface,
                        MaterialTheme.colorScheme.surfaceContainer
                    )
                )
            )
    ) {
        WavyGradientBackground {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .verticalScroll(rememberScrollState())
                    .padding(24.dp),
                verticalArrangement = Arrangement.Center,
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Spacer(modifier = Modifier.height(32.dp))

        // Animated shield icon
        Box(
            modifier = Modifier
                .size(88.dp)
                .background(
                    brush = Brush.radialGradient(
                        colors = listOf(
                            MaterialTheme.colorScheme.secondary.copy(alpha = 0.2f),
                            MaterialTheme.colorScheme.secondary.copy(alpha = 0.02f)
                        )
                    ),
                    shape = CircleShape
                ),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = Icons.Default.Security,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.secondary,
                modifier = Modifier.size(42.dp)
            )
        }

        Spacer(modifier = Modifier.height(24.dp))

        Text(
            text = stringResource(R.string.login_two_factor_title),
            fontSize = 28.sp,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.onSurface,
            textAlign = TextAlign.Center,
            lineHeight = 34.sp
        )

        Spacer(modifier = Modifier.height(8.dp))

        Text(
            text = stringResource(
                R.string.login_otp_code_requested,
                uiState.otpUsername.ifBlank { stringResource(R.string.login_otp_your_account) }
            ),
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center
        )

        Spacer(modifier = Modifier.height(32.dp))

        // OTP input card
        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(20.dp),
            colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.surfaceContainerHighest
            ),
            elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
        ) {
            Column(
                modifier = Modifier.padding(24.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(
                    text = stringResource(R.string.login_enter_verification_code),
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.onSurface
                )
                Spacer(modifier = Modifier.height(16.dp))

                OutlinedTextField(
                    value = uiState.otpToken,
                    onValueChange = {
                        // Only allow digits, max 6 chars
                        val filtered = it.filter { c -> c.isDigit() }.take(6)
                        viewModel.handleEvent(LoginEvent.UpdateOtpToken(filtered))
                    },
                    placeholder = { Text("000000") },
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(14.dp),
                    singleLine = true,
                    textStyle = MaterialTheme.typography.headlineSmall.copy(
                        textAlign = TextAlign.Center,
                        letterSpacing = 8.sp,
                        fontWeight = FontWeight.Bold
                    ),
                    keyboardOptions = KeyboardOptions(
                        keyboardType = KeyboardType.Number,
                        imeAction = ImeAction.Done
                    ),
                    keyboardActions = KeyboardActions(
                        onDone = { viewModel.handleEvent(LoginEvent.SubmitOtp) }
                    ),
                    enabled = !uiState.isLoading
                )

                Spacer(modifier = Modifier.height(20.dp))

                Button(
                    onClick = { viewModel.handleEvent(LoginEvent.SubmitOtp) },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(52.dp),
                    shape = RoundedCornerShape(14.dp),
                    enabled = !uiState.isLoading && uiState.otpToken.length >= 6
                ) {
                    if (uiState.isLoading) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(18.dp),
                            strokeWidth = 2.dp,
                            color = MaterialTheme.colorScheme.onPrimary
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(stringResource(R.string.login_verifying), fontSize = 15.sp)
                    } else {
                        Text(stringResource(R.string.login_verify_and_sign_in), fontSize = 15.sp, fontWeight = FontWeight.SemiBold)
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(24.dp))

        // Cancel / Back to credentials
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceEvenly
        ) {
            TextButton(onClick = {
                viewModel.handleEvent(LoginEvent.ResetLoginState)
            }) {
                Icon(
                    imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                    contentDescription = null,
                    modifier = Modifier.size(16.dp)
                )
                Spacer(modifier = Modifier.width(4.dp))
                Text(stringResource(R.string.login_back_to_login), fontWeight = FontWeight.Medium)
            }

            TextButton(onClick = onChangeServerConfig) {
                Icon(
                    imageVector = Icons.Default.Settings,
                    contentDescription = null,
                    modifier = Modifier.size(16.dp)
                )
                Spacer(modifier = Modifier.width(4.dp))
                Text(stringResource(R.string.login_change_server), fontWeight = FontWeight.Medium)
            }
        }

        Spacer(modifier = Modifier.height(8.dp))

        Text(
            text = if (uiState.otpToken.isNotEmpty() && uiState.otpToken.length < 6) stringResource(R.string.login_enter_all_6_digits)
            else stringResource(R.string.login_code_from_authenticator),
            style = MaterialTheme.typography.labelMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center
        )
            }
        }
    }
}


@Composable
private fun ConnectionStatusCard(
    connectionStatus: ConnectionStatus,
    onRetryClick: () -> Unit
) {
    val (statusText, statusColor, showRetryButton) = when (connectionStatus) {
        is ConnectionStatus.Connected -> Triple(stringResource(R.string.login_status_connected), MaterialTheme.colorScheme.primary, false)
        is ConnectionStatus.Connecting -> Triple(stringResource(R.string.login_status_connecting), MaterialTheme.colorScheme.tertiary, false)
        is ConnectionStatus.Disconnected -> Triple(stringResource(R.string.login_status_disconnected), MaterialTheme.colorScheme.error, true)
        is ConnectionStatus.Error -> Triple(stringResource(R.string.login_status_connection_error), MaterialTheme.colorScheme.error, true)
        is ConnectionStatus.Unknown -> Triple(stringResource(R.string.login_status_checking), MaterialTheme.colorScheme.onSurfaceVariant, false)
    }

    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = statusColor.copy(alpha = 0.1f)
        ),
        shape = RoundedCornerShape(8.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box(
                    modifier = Modifier
                        .size(8.dp)
                        .background(statusColor, shape = CircleShape)
                )

                Spacer(modifier = Modifier.width(8.dp))

                Text(
                    text = statusText,
                    style = MaterialTheme.typography.bodySmall,
                    color = statusColor,
                    fontWeight = FontWeight.Medium
                )
            }

            if (showRetryButton) {
                Text(
                    text = stringResource(R.string.common_retry),
                    style = MaterialTheme.typography.bodySmall,
                    color = statusColor,
                    fontWeight = FontWeight.Medium,
                    modifier = Modifier.clickable { onRetryClick() }
                )
            }
        }
    }
}

@Composable
private fun LoginMethodTab(
    text: String,
    isSelected: Boolean,
    onClick: () -> Unit
) {
    val backgroundColor = if (isSelected) {
        MaterialTheme.colorScheme.primary.copy(alpha = 0.1f)
    } else {
        Color.Transparent
    }

    val textColor = if (isSelected) {
        MaterialTheme.colorScheme.primary
    } else {
        MaterialTheme.colorScheme.onSurfaceVariant
    }

    Box(
        modifier = Modifier
            .clip(RoundedCornerShape(20.dp))
            .background(backgroundColor)
            .clickable { onClick() }
            .padding(horizontal = 16.dp, vertical = 8.dp)
    ) {
        Text(
            text = text,
            fontSize = 16.sp,
            fontWeight = if (isSelected) FontWeight.SemiBold else FontWeight.Normal,
            color = textColor
        )
    }
}

@Composable
private fun ServerInfoSection(onChangeServerClick: () -> Unit) {
    val context = LocalContext.current
    val (serverUrl, _) = remember { Prefs.load(context) }

    Card(
        onClick = onChangeServerClick,
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f)
        ),
        shape = RoundedCornerShape(8.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                imageVector = Icons.Default.Web,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.size(16.dp)
            )

            Spacer(modifier = Modifier.width(8.dp))

            Column {
                Text(
                    text = stringResource(R.string.login_accessing_from),
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Text(
                    text = serverUrl ?: stringResource(R.string.login_unknown_server),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.primary,
                    fontWeight = FontWeight.Medium
                )
            }
        }
    }
}
@Composable
private fun ServerConfigurationPrompt(
    onConfigureClick: () -> Unit
) {

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(
                brush = Brush.verticalGradient(
                    colors = listOf(
                        MaterialTheme.colorScheme.surface,
                        MaterialTheme.colorScheme.surfaceContainer
                    )
                )
            ),
        contentAlignment = Alignment.Center
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {

            Box(
                modifier = Modifier
                    .size(100.dp)
                    .background(
                        color = MaterialTheme.colorScheme.primaryContainer,
                        shape = RoundedCornerShape(28.dp)
                    ),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Default.Settings,
                    contentDescription = null,
                    modifier = Modifier.size(50.dp),
                    tint = MaterialTheme.colorScheme.onPrimaryContainer
                )
            }

            Spacer(modifier = Modifier.height(32.dp))

            // Title with gradient effect
            Text(
                text = stringResource(R.string.login_server_setup_required),
                style = MaterialTheme.typography.headlineMedium,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurface,
                textAlign = TextAlign.Center
            )

            Spacer(modifier = Modifier.height(12.dp))

            Text(
                text = stringResource(R.string.login_get_started),
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center,
                modifier = Modifier.padding(horizontal = 32.dp)
            )
            Spacer(modifier = Modifier.height(40.dp))
            Card(
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.surface
                ),
                elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(
                    modifier = Modifier.padding(20.dp),
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    SetupInfoItem(
                        icon = Icons.Default.Web,
                        title = stringResource(R.string.login_setup_item_url),
                        description = stringResource(R.string.login_setup_item_url_desc)
                    )

                    HorizontalDivider(
                        Modifier, DividerDefaults.Thickness, color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f)
                    )

                    SetupInfoItem(
                        icon = Icons.Default.Security,
                        title = stringResource(R.string.login_setup_item_security),
                        description = stringResource(R.string.login_setup_item_security_desc)
                    )

                    HorizontalDivider(
                        Modifier, DividerDefaults.Thickness, color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f)
                    )

                    SetupInfoItem(
                        icon = Icons.Default.Check,
                        title = stringResource(R.string.login_setup_item_quick),
                        description = stringResource(R.string.login_setup_item_quick_desc)
                    )
                }
            }
            Spacer(modifier = Modifier.height(32.dp))
            Button(
                onClick = onConfigureClick,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(56.dp),
                shape = RoundedCornerShape(16.dp),
                elevation = ButtonDefaults.buttonElevation(
                    defaultElevation = 4.dp,
                    pressedElevation = 8.dp
                )
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Settings,
                        contentDescription = null,
                        modifier = Modifier.size(20.dp)
                    )
                    Text(
                        stringResource(R.string.login_configure_server),
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.SemiBold
                    )
                }
            }

            Spacer(modifier = Modifier.height(16.dp))
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.Center
            ) {
                Icon(
                    imageVector = Icons.Default.Info,
                    contentDescription = null,
                    modifier = Modifier.size(16.dp),
                    tint = MaterialTheme.colorScheme.primary
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = stringResource(R.string.login_first_time_guide),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.primary,
                    fontWeight = FontWeight.Medium
                )
            }
        }
    }
}

@Composable
private fun SetupInfoItem(
    icon: ImageVector,
    title: String,
    description: String
) {
    Row(
        verticalAlignment = Alignment.Top,
        horizontalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        Box(
            modifier = Modifier
                .size(40.dp)
                .background(
                    color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.5f),
                    shape = RoundedCornerShape(12.dp)
                ),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                modifier = Modifier.size(20.dp),
                tint = MaterialTheme.colorScheme.primary
            )
        }

        Column {
            Text(
                text = title,
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.SemiBold,
                color = MaterialTheme.colorScheme.onSurface
            )
            Text(
                text = description,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}