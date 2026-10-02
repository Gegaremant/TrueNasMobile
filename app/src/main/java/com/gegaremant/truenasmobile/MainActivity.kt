package com.gegaremant.truenasmobile

import android.Manifest
import android.app.Application
import android.content.Intent
import android.os.Build
import android.os.Bundle
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.biometric.BiometricManager
import androidx.biometric.BiometricPrompt
import androidx.compose.animation.AnimatedContentTransitionScope
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import androidx.core.view.WindowCompat
import androidx.fragment.app.FragmentActivity
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.NavHostController
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.google.accompanist.permissions.ExperimentalPermissionsApi
import com.google.accompanist.permissions.isGranted
import com.google.accompanist.permissions.rememberPermissionState
import com.gegaremant.truenasmobile.data.api.TrueNASApiManager
import com.gegaremant.truenasmobile.data.helpers.PersonalizationManager
import com.gegaremant.truenasmobile.data.helpers.ThemeMode
import com.gegaremant.truenasmobile.data.helpers.dataStore
import com.gegaremant.truenasmobile.data.security.BiometricLockPrefs
import com.gegaremant.truenasmobile.data.security.shouldReLock
import com.gegaremant.truenasmobile.R
import com.gegaremant.truenasmobile.ui.MainScreen
import com.gegaremant.truenasmobile.ui.Screen
import com.gegaremant.truenasmobile.ui.account.AccountSwitcherScreen
import com.gegaremant.truenasmobile.ui.components.LoadingScreen
import com.gegaremant.truenasmobile.ui.components.ModernToastHost
import com.gegaremant.truenasmobile.ui.components.NoInternetScreen
import com.gegaremant.truenasmobile.ui.components.ToastManager
import com.gegaremant.truenasmobile.ui.homepage.instancesettings.alertservice.AlertClassesConfigScreen
import com.gegaremant.truenasmobile.ui.homepage.instancesettings.alertservice.AlertServiceCreateScreen
import com.gegaremant.truenasmobile.ui.homepage.instancesettings.alertservice.AlertServiceDetailScreen
import com.gegaremant.truenasmobile.ui.homepage.instancesettings.alertservice.AlertServicesListScreen
import com.gegaremant.truenasmobile.ui.login.LoginScreen
import com.gegaremant.truenasmobile.ui.profile.ProfileScreen
import com.gegaremant.truenasmobile.ui.settings.SettingsEvent
import com.gegaremant.truenasmobile.ui.settings.SettingsScreen
import com.gegaremant.truenasmobile.ui.settings.SettingsScreenViewModel
import com.gegaremant.truenasmobile.ui.settings.push.PushSettingsScreen
import com.gegaremant.truenasmobile.ui.settings.screens.AboutScreen
import com.gegaremant.truenasmobile.ui.settings.logging.AppLoggingScreen
import com.gegaremant.truenasmobile.ui.settings.screens.LicensesScreen
import com.gegaremant.truenasmobile.ui.settings.screens.ThemeScreen
import com.gegaremant.truenasmobile.ui.settings.sheets.ChangePasswordScreen
import com.gegaremant.truenasmobile.ui.security.BiometricLockScreen
import com.gegaremant.truenasmobile.ui.theme.TrueNasMobileAppTheme
import kotlinx.coroutines.launch

class MainActivity : FragmentActivity() {

    private val viewModel: MainViewModel by viewModels()

    // Local app-lock state (biometric / device credential).
    private val appUnlocked = mutableStateOf(false)
    private val lockResolved = mutableStateOf(false)

    /**
     * Cached "is app lock enabled" flag.
     *
     * It used to be read once in [onCreate] and then forgotten, so [onResume]
     * re-armed the lock without ever consulting it: returning from the
     * background after [AUTO_LOCK_DELAY_MS] showed the lock screen and asked
     * for biometrics even when the user had the lock switched off - with no
     * way out of it, because the lock screen has no back affordance.
     */
    private val lockEnabled = mutableStateOf(false)
    private var lastBackgroundedAt: Long? = null
    private var lockErrorMessage: String? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        handleWidgetIntent(intent)
        lifecycleScope.launch {
            val enabled = BiometricLockPrefs.isEnabled(this@MainActivity.dataStore)
            lockEnabled.value = enabled
            if (!enabled) {
                appUnlocked.value = true
            }
            lockResolved.value = true
        }
        setContent {
            val personalization by PersonalizationManager.state.collectAsState()
            val systemDark = androidx.compose.foundation.isSystemInDarkTheme()
            val darkTheme = when (personalization.themeMode) {
                com.gegaremant.truenasmobile.data.helpers.ThemeMode.LIGHT -> false
                com.gegaremant.truenasmobile.data.helpers.ThemeMode.DARK -> true
                com.gegaremant.truenasmobile.data.helpers.ThemeMode.SYSTEM -> systemDark
            }
            TrueNasMobileAppTheme(
                theme = personalization.theme,
                darkTheme = darkTheme,
                isBlackMode = personalization.blackMode
            ) {
                when {
                    !lockResolved.value -> LoadingScreen(stringResource(R.string.startup_preparing))
                    !appUnlocked.value -> BiometricLockScreen(
                        statusMessage = lockErrorMessage,
                        onUnlock = { requestBiometricUnlock() }
                    )
                    else -> MainActivityContent(
                        viewModel = viewModel
                    )
                }
            }
        }
    }

    override fun onStop() {
        super.onStop()
        lastBackgroundedAt = System.currentTimeMillis()
    }

    override fun onResume() {
        super.onResume()
        val backgroundedAt = lastBackgroundedAt
        if (lockEnabled.value &&
            shouldReLock(backgroundedAt, System.currentTimeMillis(), AUTO_LOCK_DELAY_MS)
        ) {
            lockErrorMessage = null
            appUnlocked.value = false
        }
        lastBackgroundedAt = null
    }

    private fun requestBiometricUnlock() {
        // Defence in depth: no future call site can reach the system prompt
        // while the lock is switched off.
        if (!lockEnabled.value) {
            lockErrorMessage = null
            appUnlocked.value = true
            return
        }
        try {
            val promptInfo = BiometricPrompt.PromptInfo.Builder()
                .setTitle(getString(R.string.biometric_prompt_title))
                .setSubtitle(getString(R.string.biometric_prompt_subtitle))
                // NOTE: androidx.biometric forbids a negative button when
                // DEVICE_CREDENTIAL is among the allowed authenticators.
                .setAllowedAuthenticators(
                    BiometricManager.Authenticators.BIOMETRIC_STRONG or
                        BiometricManager.Authenticators.DEVICE_CREDENTIAL
                )
                .build()
            val prompt = BiometricPrompt(
                this,
                ContextCompat.getMainExecutor(this),
                object : BiometricPrompt.AuthenticationCallback() {
                    override fun onAuthenticationSucceeded(result: BiometricPrompt.AuthenticationResult) {
                        lockErrorMessage = null
                        appUnlocked.value = true
                    }

                    override fun onAuthenticationError(errorCode: Int, errString: CharSequence) {
                        lockErrorMessage = errString.toString()
                    }
                }
            )
            prompt.authenticate(promptInfo)
        } catch (e: Exception) {
            lockErrorMessage = getString(R.string.biometric_unavailable, e.message)
        }
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        setIntent(intent)
        handleWidgetIntent(intent)
    }

    private fun handleWidgetIntent(intent: Intent?) {
        when (intent?.action) {
            "com.gegaremant.truenasmobile.OPEN_APPS" ->
                viewModel.requestNavigateTo(Screen.Apps.route)
            "com.gegaremant.truenasmobile.OPEN_SYSTEM_UPDATE" ->
                viewModel.requestNavigateTo(Screen.SystemUpdateScreen.route)
            "com.gegaremant.truenasmobile.OPEN_INSTALL_APP" ->
                viewModel.requestNavigateTo(Screen.Marketplace.route)
            "com.gegaremant.truenasmobile.OPEN_UPDATE_APPS" ->
                viewModel.requestNavigateTo(Screen.Apps.route)
            "com.gegaremant.truenasmobile.OPEN_CONFIGURE_INSTANCE" ->
                viewModel.requestNavigateTo(Screen.InstanceConfigScreen.route)
            "com.gegaremant.truenasmobile.OPEN_UPDATE_INSTANCE" ->
                viewModel.requestNavigateTo(Screen.SystemUpdateScreen.route)
        }
    }

    override fun onDestroy() {
        super.onDestroy()
    }
}

@OptIn(ExperimentalPermissionsApi::class)
@Composable
fun MainActivityContent(
    viewModel: MainViewModel
) {
    val context = LocalContext.current
    val appState by viewModel.appState.collectAsState()
    val manager by viewModel.manager.collectAsState()
    val navController = rememberNavController()
    val localNetworkPermission = if (
        Build.VERSION.SDK_INT >= Build.VERSION_CODES.CINNAMON_BUN
    ) {
        rememberPermissionState(
            permission = Manifest.permission.ACCESS_LOCAL_NETWORK,
            onPermissionResult = {
                viewModel.initializeApp(context)
            }
        )
    } else {
        null
    }

    LaunchedEffect(Unit) {
        if (localNetworkPermission != null && !localNetworkPermission.status.isGranted) {
            localNetworkPermission.launchPermissionRequest()
        } else {
            viewModel.initializeApp(context)
        }
    }
    LaunchedEffect(manager) {
        manager?.let {
            viewModel.startPeriodicPing(context)
            viewModel.startPeriodicAppSync(context)
            // Log the other saved profiles in behind the user's back, so
            // switching to one of them later costs nothing.
            viewModel.warmOtherProfiles(context)
        }
    }

    Box(modifier = Modifier.fillMaxSize()) {
        when (appState) {
            is AppState.Initializing -> LoadingScreen(stringResource(R.string.startup_initializing))
            is AppState.CheckingConnection -> LoadingScreen(stringResource(R.string.common_connecting_to_server))
            is AppState.ValidatingToken -> LoadingScreen(stringResource(R.string.startup_validating_credentials))
            is AppState.AttemptingAutoLogin -> LoadingScreen(stringResource(R.string.startup_attempting_auto_login))
            is AppState.Ready -> {
                AppNavigation(
                    startRoute = (appState as AppState.Ready).startRoute,
                    navController = navController,
                    viewModel = viewModel,
                    manager = manager
                )
            }
            is AppState.Error -> {
                LaunchedEffect((appState as AppState.Error).message) {
                    ToastManager.showError((appState as AppState.Error).message)
                }
                AppNavigation(
                    startRoute = (appState as AppState.Error).fallbackRoute,
                    navController = navController,
                    viewModel = viewModel,
                    manager = manager
                )
            }
            is AppState.NoInternet -> {
                TrueNasMobileAppTheme {
                    NoInternetScreen(
                        message = stringResource(R.string.startup_no_internet),
                        onRetry = {
                            viewModel.initializeApp(context)
                        }
                    )
                }
            }
            is AppState.TotpRequired -> {
                AppNavigation(
                    startRoute = Screen.Login.route,
                    navController = navController,
                    viewModel = viewModel,
                    manager = manager,
                    totpUsername = (appState as AppState.TotpRequired).username
                )
            }
        }

        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(top = 16.dp)
        ) {
            ModernToastHost()
        }
    }
}

@Composable
private fun AppNavigation(
    startRoute: String,
    navController: NavHostController,
    viewModel: MainViewModel,
    manager: TrueNASApiManager?,
    totpUsername: String? = null
) {
    val context = LocalContext.current
    val pendingNav by viewModel.pendingNavigation.collectAsState()
    val personalization by PersonalizationManager.state.collectAsState()
    val userKey by viewModel.currentUserKey.collectAsState()
    val personalizationUserKey = userKey ?: PersonalizationManager.DEFAULT_USER_KEY
    LaunchedEffect(pendingNav) {
        val route = pendingNav ?: return@LaunchedEffect
        if (navController.currentDestination?.route != Screen.Main.route) {
            navController.navigate(Screen.Main.route) {
                popUpTo(navController.graph.findStartDestination().id) { saveState = true }
                launchSingleTop = true
            }
        }
    }

    NavHost(
        navController = navController,
        startDestination = startRoute,
        enterTransition = {
            fadeIn(animationSpec = tween(300)) + slideIntoContainer(AnimatedContentTransitionScope.SlideDirection.Start, animationSpec = spring(stiffness = Spring.StiffnessMediumLow))
        },
        exitTransition = {
            fadeOut(animationSpec = tween(300)) + slideOutOfContainer(AnimatedContentTransitionScope.SlideDirection.Start, animationSpec = spring(stiffness = Spring.StiffnessMediumLow))
        },
        popEnterTransition = {
            fadeIn(animationSpec = tween(300)) + slideIntoContainer(AnimatedContentTransitionScope.SlideDirection.End, animationSpec = spring(stiffness = Spring.StiffnessMediumLow))
        },
        popExitTransition = {
            fadeOut(animationSpec = tween(300)) + slideOutOfContainer(AnimatedContentTransitionScope.SlideDirection.End, animationSpec = spring(stiffness = Spring.StiffnessMediumLow))
        }
    ) {
        composable(Screen.Login.route) {
            LoginScreen(
                existingManager = manager,
                navController = navController,
                onManagerInitialized = { newManager ->
                    viewModel.updateManager(newManager)
                },
                onLoginSuccess = {
                    navController.navigate(Screen.Main.route) {
                        popUpTo(Screen.Login.route) { inclusive = true }
                        launchSingleTop = true
                        anim {
                            enter = 0
                            exit = 0
                            popEnter = 0
                            popExit = 0
                        }
                    }
                },
                startInOtpMode = totpUsername != null,
                totpUsername = totpUsername
            )
        }

        composable(Screen.AccountSwitcher.route) {
            AccountSwitcherScreen(
                onAccountSelected = { server, account ->
                    (context as? FragmentActivity)?.lifecycleScope?.launch {
                        val loginManager = viewModel.attemptLoginWithProfile(context, server, account)
                        if (loginManager != null) {
                            viewModel.updateManager(loginManager)
                            navController.navigate(Screen.Main.route) {
                                popUpTo(Screen.AccountSwitcher.route) { inclusive = true }
                            }
                        } else {
                            ToastManager.showErrorRes(R.string.startup_failed_login_saved_account)
                            navController.navigate(Screen.Login.route) {
                                popUpTo(Screen.AccountSwitcher.route) { inclusive = true }
                            }
                        }
                    }
                },
                onAddNewAccount = {
                    navController.navigate(Screen.Login.route)
                }
            )
        }

        composable(Screen.Main.route) {
            manager?.let { validManager ->
                MainScreen(
                    manager = validManager,
                    rootNavController = navController,
                    viewModel = viewModel
                )
            } ?: run {
                LaunchedEffect(Unit) {
                    ToastManager.showErrorRes(R.string.startup_session_invalid)
                    navController.navigate(Screen.Login.route) {
                        popUpTo(Screen.Main.route) { inclusive = true }
                    }
                }
                LoadingScreen(stringResource(R.string.startup_redirecting_to_login))
            }
        }

        composable(Screen.Settings.route) {
            SettingsScreen(
                manager = manager,
                onNavigateToTheme = {
                    navController.navigate(Screen.Theme.route)
                },
                onNavigateToAbout = {
                    navController.navigate(Screen.About.route)
                },
                onNavigateToLicenses = {
                    navController.navigate(Screen.Licenses.route)
                },
                onNavigateToLogging = {
                    navController.navigate(Screen.AppLogging.route)
                },
                onNavigateToLogin = {
                    navController.navigate(Screen.AccountSwitcher.route) {
                        popUpTo(Screen.Settings.route) { inclusive = true }
                    }
                },
                onNavigateToChangePassword = {
                    navController.navigate(Screen.ChangePassword.route)
                },
                onNavigateToProfile = {
                    navController.navigate(Screen.Profile.route)
                },
                onNavigateToPushSettings = {
                    navController.navigate(Screen.PushSettings.route)
                },
                onNavigateBack = {
                    navController.popBackStack()
                }
            )
        }
        composable(Screen.Profile.route) {
            manager?.let {
                ProfileScreen(
                    manager = it,
                    onSettingsClick = { navController.navigate(Screen.Settings.route) },
                    onNavigateBack = { navController.popBackStack() }
                )
            }
        }
        composable(Screen.PushSettings.route) {
            PushSettingsScreen(
                manager = manager,
                onNavigateBack = { navController.popBackStack() }
            )
        }
        composable(Screen.AppLogging.route) {
            manager?.let {
                AppLoggingScreen(
                    manager = it,
                    onNavigateBack = { navController.popBackStack() }
                )
            }
        }
        composable(Screen.ChangePassword.route) {
            val context = LocalContext.current
            val settingsViewModel: SettingsScreenViewModel = viewModel(
                factory = SettingsScreenViewModel.SettingsViewModelFactory(
                    manager = manager!!,
                    application = context.applicationContext as Application
                )
            )
            ChangePasswordScreen(
                manager,
                { oldPassword, newPassword ->
                    settingsViewModel.handleEvent(
                        SettingsEvent.ChangePassword(oldPassword, newPassword)
                    )
                },
                { navController.popBackStack() }
            )
        }
        composable(Screen.AlertServicesList.route) {
            manager?.let { validManager ->
                AlertServicesListScreen(
                    manager = validManager,
                    onNavigateBack = { navController.popBackStack() },
                    onNavigateToDetail = { service ->
                        navController.navigate(Screen.AlertServiceDetail.createRoute(service.id))
                    },
                    onNavigateToCreate = {
                        navController.navigate(Screen.AlertServiceCreate.route)
                    },
                    onNavigateToClassesConfig = { navController.navigate(Screen.AlertClassesConfig.route) }
                )
            }
        }
        composable(
            Screen.AlertServiceDetail.route,
            arguments = listOf(navArgument("serviceId") { type = NavType.IntType })
        ) { backStackEntry ->
            val serviceId = backStackEntry.arguments?.getInt("serviceId") ?: return@composable
            manager?.let { validManager ->
                AlertServiceDetailScreen(
                    serviceId = serviceId,
                    manager = validManager,
                    onNavigateBack = { navController.popBackStack() }
                )
            }
        }
        composable(Screen.AlertServiceCreate.route) {
            manager?.let { validManager ->
                AlertServiceCreateScreen(
                    manager = validManager,
                    onNavigateBack = { navController.popBackStack() }
                )
            }
        }
        composable(Screen.AlertClassesConfig.route) {
            manager?.let {
                AlertClassesConfigScreen(
                    manager = manager,
                    onNavigateBack = { navController.popBackStack() }
                )
            }
        }
        composable(Screen.About.route) {
            manager?.let { validManager ->
                AboutScreen(
                    manager = validManager,
                    onNavigateBack = { navController.popBackStack() }
                )
            } ?: run {
                LoadingScreen(stringResource(R.string.startup_redirecting))
                LaunchedEffect(Unit) { navController.popBackStack() }
            }
        }

        composable(Screen.Licenses.route) {
            manager?.let { validManager ->
                LicensesScreen(
                    manager = validManager,
                    onNavigateBack = { navController.popBackStack() }
                )
            } ?: run {
                LoadingScreen(stringResource(R.string.startup_redirecting))
                LaunchedEffect(Unit) { navController.popBackStack() }
            }
        }

        composable(Screen.Theme.route) {
            ThemeScreen(
                manager = manager,
                userKey = personalizationUserKey,
                currentTheme = personalization.theme,
                onThemeSelected = { newTheme ->
                    val userKey = viewModel.currentUserKey.value ?: PersonalizationManager.DEFAULT_USER_KEY
                    PersonalizationManager.saveTheme(context, userKey, newTheme)
                },
                onNavigateBack = { navController.popBackStack() },
                themeMode = personalization.themeMode,
                onThemeModeSelected = { mode ->
                    val userKey = viewModel.currentUserKey.value ?: PersonalizationManager.DEFAULT_USER_KEY
                    PersonalizationManager.saveThemeMode(context, userKey, mode)
                }
            )
        }
    }
}

/** Auto-lock the app when it has been backgrounded for at least this long. */
private const val AUTO_LOCK_DELAY_MS = 30_000L