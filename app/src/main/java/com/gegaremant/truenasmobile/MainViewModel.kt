package com.gegaremant.truenasmobile

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.gegaremant.truenasmobile.data.ApiResult
import com.gegaremant.truenasmobile.data.TrueNASClient
import com.gegaremant.truenasmobile.data.api.AuthService
import com.gegaremant.truenasmobile.data.api.TrueNASApiManager
import com.gegaremant.truenasmobile.data.helpers.AccountSessionRegistry
import com.gegaremant.truenasmobile.data.helpers.MultiAccountPrefs
import com.gegaremant.truenasmobile.data.helpers.NetworkConnectivityObserver
import com.gegaremant.truenasmobile.data.helpers.PersonalizationManager
import com.gegaremant.truenasmobile.data.models.Config.ClientConfig
import com.gegaremant.truenasmobile.data.models.LoginExResult
import com.gegaremant.truenasmobile.data.models.LoginMechanisms
import com.gegaremant.truenasmobile.data.models.LoginMethod
import com.gegaremant.truenasmobile.data.models.SavedAccount
import com.gegaremant.truenasmobile.data.models.SavedServer
import com.gegaremant.truenasmobile.data.workers.AppsRefreshWorker
import com.gegaremant.truenasmobile.ui.Screen
import com.gegaremant.truenasmobile.ui.utils.AppCache
import com.gegaremant.truenasmobile.ui.utils.NavigationHolders
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withTimeoutOrNull
import kotlin.time.Duration.Companion.milliseconds

sealed class AppState {
    object Initializing : AppState()
    object CheckingConnection : AppState()
    object ValidatingToken : AppState()
    object NoInternet : AppState()
    object AttemptingAutoLogin : AppState()
    data class Ready(val startRoute: String) : AppState()
    data class Error(val message: String, val fallbackRoute: String) : AppState()
    data class TotpRequired(val username: String) : AppState()
}

enum class TotpResult { OTP_REQUIRED, SUCCESS }

class MainViewModel : ViewModel() {

    private companion object {
        /** Pause between warming one profile and the next, so warming is not a burst. */
        const val WARM_PROFILE_GAP_MILLIS = 1500L

        /**
         * Shorter than the interactive path: a warmed profile that is not ready
         * in ten seconds is not going to make anybody's switch faster, and the
         * user may be tapping a third profile meanwhile.
         */
        const val WARM_CONNECT_TIMEOUT_MILLIS = 10_000L
    }

    private val _appState = MutableStateFlow<AppState>(AppState.Initializing)
    val appState: StateFlow<AppState> = _appState.asStateFlow()

    private val _manager = MutableStateFlow<TrueNASApiManager?>(null)
    val manager: StateFlow<TrueNASApiManager?> = _manager.asStateFlow()

    // Active user key used to scope personalization (theme, navbar, etc.).
    private val _currentUserKey = MutableStateFlow<String?>(null)
    val currentUserKey: StateFlow<String?> = _currentUserKey.asStateFlow()

    private var hasInitialized = false

    /** Guards against a second warming pass while the first is still running. */
    private var warmJob: Job? = null
    private val _pendingNavigation = MutableStateFlow<String?>(null)
    val pendingNavigation: StateFlow<String?> = _pendingNavigation.asStateFlow()

    fun requestNavigateTo(route: String) {
        _pendingNavigation.value = route
    }

    fun clearPendingNavigation() {
        _pendingNavigation.value = null
    }

    /** Set the active user and load their personalization from disk. */
    fun setActiveUser(context: Context, accountId: String?) {
        val key = accountId ?: PersonalizationManager.DEFAULT_USER_KEY
        _currentUserKey.value = key
        PersonalizationManager.loadForUser(context, key)
    }
    fun initializeApp(context: Context) {
        if (hasInitialized) return
        hasInitialized = true
        viewModelScope.launch {
            try {
                _appState.value = AppState.Initializing
                val networkUtils = NetworkConnectivityObserver(context)
                if (!networkUtils.isNetworkAvailable()) {
                    _appState.value = AppState.NoInternet
                    return@launch
                }

                val hasSavedAccounts = MultiAccountPrefs.getAccounts(context).isNotEmpty()

                if (hasSavedAccounts) {
                    _appState.value = AppState.ValidatingToken

                    val (serverId, accountId) = MultiAccountPrefs.getLastUsedProfile(context) ?: run {
                        _appState.value = AppState.Ready(Screen.AccountSwitcher.route)
                        return@launch
                    }

                    val server = MultiAccountPrefs.getServer(context, serverId)
                    val account = MultiAccountPrefs.getAccount(context, accountId)
                    val token = MultiAccountPrefs.getTokenForLastUsed(context)

                    if (server != null && account != null) {
                        var manager: TrueNASApiManager? = null

                        // Phase 1: try saved token
                        if (token != null) {
                            manager = attemptLoginWithToken(context, server, account, token)
                            if (manager != null) {
                                _manager.value = manager
                                setActiveUser(context, accountId)
                                _appState.value = AppState.Ready(Screen.Main.route)
                                return@launch
                            }
                        }

                        // Phase 2: token failed or missing — try loginEx with saved password
                        val totpManager = attemptTotpAutoLogin(context, server, account)
                        if (totpManager != null) {
                            when (totpManager.second) {
                                TotpResult.OTP_REQUIRED -> {
                                    _appState.value = AppState.TotpRequired(account.username)
                                    return@launch
                                }
                                TotpResult.SUCCESS -> {
                                    _manager.value = totpManager.first
                                    setActiveUser(context, accountId)
                                    _appState.value = AppState.Ready(Screen.Main.route)
                                    return@launch
                                }
                            }
                        }
                    }
                    _appState.value = AppState.Ready(Screen.AccountSwitcher.route)

                } else {
                    _appState.value = AppState.Ready(Screen.Login.route)
                }

            } catch (e: Exception) {
                _appState.value = AppState.Error(
                    "Initialization failed: ${e.message}",
                    Screen.Login.route
                )
            }
        }
    }

    fun updateManager(newManager: TrueNASApiManager) {
        _manager.value = newManager
    }
    fun startPeriodicAppSync(context: Context) {
        AppsRefreshWorker.scheduleRecurring(context)
        AppsRefreshWorker.scheduleImmediate(context)
    }

    fun startPeriodicPing(context: Context) {
        viewModelScope.launch {
            while (true) {
                try {
                    val authToken = MultiAccountPrefs.getTokenForLastUsed(context)
                    val currentManager = _manager.value

                    if (authToken != null && currentManager?.isConnected() == true) {
                        val result = currentManager.connection.pingConnectionWithResult()
                        // A successful ping is proof of life for the registry, so
                        // switching back to this profile later stays free instead
                        // of spending a round trip re-checking the same socket.
                        if (result is ApiResult.Success) {
                            MultiAccountPrefs.getLastUsedProfile(context)?.let { (_, accountId) ->
                                AccountSessionRegistry.markAlive(accountId)
                            }
                        }
                    }
                } catch (_: Exception) {
                }
                delay(30000.milliseconds)
            }
        }
    }

    suspend fun attemptLoginWithProfile(
        context: Context,
        server: SavedServer,
        account: SavedAccount
    ): TrueNASApiManager? {
        // Fast path: this account is already connected, either because the user
        // was just here or because warming got there first. No handshake, no
        // login, no token generation.
        AccountSessionRegistry.acquire(account.id)?.let { warm ->
            return activateProfile(context, server, account, warm)
        }

        return try {
            val config = ClientConfig(
                serverUrl = server.serverUrl,
                insecure = server.insecure,
                connectionTimeoutMs = 10000,
                enablePing = true,
                enableDebugLogging = false
            )

            val client = TrueNASClient(config)
            val manager = TrueNASApiManager(client, context.applicationContext)

            if (!manager.connect()) return null

            val (cred1, cred2) = MultiAccountPrefs.getAccountCredentials(
                context,
                account.id,
                account.loginMethod
            )

            val loginSuccess = when (account.loginMethod) {
                LoginMethod.API_KEY -> {
                    cred1?.let {
                        val result = manager.auth.loginWithApiKeyWithResult(it)
                        result is ApiResult.Success && result.data
                    } ?: false
                }
                LoginMethod.PASSWORD, LoginMethod.TOTP -> {
                    if (cred1 != null && cred2 != null) {
                        val result = manager.auth.loginUserWithResult(
                            AuthService.DefaultAuth(cred1, cred2)
                        )
                        result is ApiResult.Success && result.data
                    } else false
                }
            }

            if (loginSuccess) {
                val tokenResult = manager.auth.generateTokenWithResult()
                if (tokenResult is ApiResult.Success) {
                    MultiAccountPrefs.saveCurrentSession(
                        context,
                        server.id,
                        account.id,
                        tokenResult.data
                    )
                    activateProfile(context, server, account, manager)
                } else {
                    client.disconnect()
                    null
                }
            } else {
                client.disconnect()
                null
            }

        } catch (_: Exception) {
            null
        }
    }

    /**
     * Makes [account] the active profile, whichever way its manager was obtained.
     *
     * The last-used pointer is the app's idea of "who is logged in": seventeen
     * places read it - app start, both widgets, the AI app functions, the alert
     * and job workers, session recovery. The switcher used to skip it, so after
     * switching accounts the app still considered the *previous* profile active:
     * a relaunch logged back into the old NAS, the background workers polled its
     * pools and sent its alerts, and an expired session recovered with the wrong
     * credentials.
     *
     * The cache is one process-wide bucket, so it has to be dropped as well -
     * the data in it belongs to the server we just left.
     */
    private suspend fun activateProfile(
        context: Context,
        server: SavedServer,
        account: SavedAccount,
        manager: TrueNASApiManager
    ): TrueNASApiManager {
        MultiAccountPrefs.saveLastUsedProfile(context, server.id, account.id)
        setActiveUser(context, account.id)
        AppCache.clearAllCache()
        NavigationHolders.clearAll()
        AccountSessionRegistry.remember(account.id, manager)
        return manager
    }

    /**
     * Logs the other saved profiles in while the user is busy on this one.
     *
     * The point is that tapping a profile the user has visited before costs
     * nothing; without this, only the profile they happen to log into first is
     * ever warm.
     *
     * Deliberately side-effect free: no session is written, no last-used
     * pointer is moved and no active user is changed. Warming a profile must not
     * be able to make it the current one. Accounts are warmed one at a time with
     * a gap, so a user with ten saved NASes does not open ten sockets at once,
     * and a failure is per-account - an unreachable NAS costs its own entry
     * nothing else.
     */
    fun warmOtherProfiles(context: Context) {
        if (warmJob?.isActive == true) return
        warmJob = viewModelScope.launch {
            try {
                val (lastServerId, lastAccountId) =
                    MultiAccountPrefs.getLastUsedProfile(context) ?: return@launch

                // The manager that is live right now belongs to the active
                // profile; without this the login screen's manager would be
                // warmed again as if it were somebody else's.
                _manager.value?.let { AccountSessionRegistry.remember(lastAccountId, it) }

                for (account in MultiAccountPrefs.getAccounts(context)) {
                    if (account.id == lastAccountId) continue
                    if (AccountSessionRegistry.trackedAccountIds.contains(account.id) &&
                        AccountSessionRegistry.acquire(account.id) != null
                    ) continue
                    val server = MultiAccountPrefs.getServer(context, account.serverId) ?: continue
                    warmProfile(context, server, account)
                    delay(WARM_PROFILE_GAP_MILLIS)
                }
            } catch (_: Exception) {
                // Warming is an optimisation; it must never surface an error.
            }
        }
    }

    private suspend fun warmProfile(
        context: Context,
        server: SavedServer,
        account: SavedAccount
    ) {
        var client: TrueNASClient? = null
        try {
            val config = ClientConfig(
                serverUrl = server.serverUrl,
                insecure = server.insecure,
                connectionTimeoutMs = WARM_CONNECT_TIMEOUT_MILLIS,
                // Same client configuration as the interactive path: the warm
                // socket has to behave like the one the user would have got.
                enablePing = true,
                enableDebugLogging = false
            )
            val newClient = TrueNASClient(config)
            client = newClient
            val manager = TrueNASApiManager(newClient, context.applicationContext)
            if (!manager.connect()) return

            val (cred1, cred2) = MultiAccountPrefs.getAccountCredentials(
                context,
                account.id,
                account.loginMethod
            )
            val authenticated = when (account.loginMethod) {
                LoginMethod.API_KEY -> {
                    cred1?.let {
                        val result = manager.auth.loginWithApiKeyWithResult(it)
                        result is ApiResult.Success && result.data
                    } ?: false
                }
                LoginMethod.PASSWORD, LoginMethod.TOTP -> {
                    if (cred1 != null && cred2 != null) {
                        val result = manager.auth.loginUserWithResult(
                            AuthService.DefaultAuth(cred1, cred2)
                        )
                        result is ApiResult.Success && result.data
                    } else false
                }
            }

            if (authenticated) {
                AccountSessionRegistry.remember(account.id, manager)
            } else {
                newClient.disconnect()
            }
        } catch (_: Exception) {
            client?.disconnect()
        }
    }

    /**
     * Connects to server, fires loginEx with saved password credentials.
     * Returns Pair(manager, TotpResult.OTP_REQUIRED) if the server asks for TOTP.
     * Returns Pair(manager, TotpResult.SUCCESS) if login succeeds and a fresh token is generated.
     * Returns null on any other failure.
     */
    private suspend fun attemptTotpAutoLogin(
        context: Context,
        server: SavedServer,
        account: SavedAccount
    ): Pair<TrueNASApiManager, TotpResult>? {
        return withTimeoutOrNull(15000.milliseconds) {
            try {
                val config = ClientConfig(
                    serverUrl = server.serverUrl,
                    insecure = server.insecure,
                    connectionTimeoutMs = 5000,
                    enablePing = true,
                    enableDebugLogging = false
                )
                val client = TrueNASClient(config)
                val manager = TrueNASApiManager(client, context)
                if (!manager.connect()) return@withTimeoutOrNull null

                val (cred1, cred2) = MultiAccountPrefs.getAccountCredentials(
                    context, account.id, account.loginMethod
                )
                if (cred1 == null || cred2 == null) return@withTimeoutOrNull null

                val mechanism = LoginMechanisms.AuthPasswordPlain(
                    username = cred1,
                    password = cred2,
                    login_options = LoginMechanisms.LoginOptions(user_info = true)
                )
                val result = manager.auth.loginEx(mechanism, includeUserInfo = true)

                when {
                    result is ApiResult.Success && result.data is LoginExResult.AuthRespOTPRequired -> {
                        _manager.value = manager
                        Pair(manager, TotpResult.OTP_REQUIRED)
                    }
                    result is ApiResult.Success && result.data is LoginExResult.AuthRespSuccess -> {
                        // loginEx succeeded directly — generate token and save
                        val tokenResult = manager.auth.generateTokenWithResult()
                        if (tokenResult is ApiResult.Success) {
                            MultiAccountPrefs.saveCurrentSession(
                                context, server.id, account.id, tokenResult.data
                            )
                            // Also save credentials to make sure they're persisted
                            MultiAccountPrefs.saveAccountCredentials(
                                context, account.id, account.loginMethod,
                                username = cred1, password = cred2
                            )
                            _manager.value = manager
                            Pair(manager, TotpResult.SUCCESS)
                        } else null
                    }
                    else -> null
                }
            } catch (_: Exception) {
                null
            }
        }
    }

    private suspend fun attemptLoginWithToken(
        context: Context,
        server: SavedServer,
        account: SavedAccount,
        token: String
    ): TrueNASApiManager? {
        return withTimeoutOrNull(10000.milliseconds) {
            try {
                val config = ClientConfig(
                    serverUrl = server.serverUrl,
                    insecure = server.insecure,
                    connectionTimeoutMs = 5000,
                    enablePing = true,
                    enableDebugLogging = false
                )

                val client = TrueNASClient(config)
                val manager = TrueNASApiManager(client, context)

                if (!manager.connect()) return@withTimeoutOrNull null

                val tryLogin = manager.auth.loginWithTokenAndResult(token)
                if (tryLogin is ApiResult.Error) return@withTimeoutOrNull null

                val newTokenResult = manager.auth.generateTokenWithResult()
                if (newTokenResult is ApiResult.Success) {
                    MultiAccountPrefs.saveTokenForLastUsed(
                        context,
                        newTokenResult.data
                    )
                    manager
                } else {
                    null
                }

            } catch (_: Exception) {
                null
            }
        }
    }
}