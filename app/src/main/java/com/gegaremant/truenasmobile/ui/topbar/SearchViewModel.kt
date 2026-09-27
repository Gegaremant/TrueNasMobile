package com.gegaremant.truenasmobile.ui.topbar

import androidx.annotation.StringRes
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.gegaremant.truenasmobile.R
import com.gegaremant.truenasmobile.data.ApiResult
import com.gegaremant.truenasmobile.data.api.TrueNASApiManager
import com.gegaremant.truenasmobile.data.models.Apps
import com.gegaremant.truenasmobile.data.models.Shares
import com.gegaremant.truenasmobile.data.models.System
import com.gegaremant.truenasmobile.data.models.Virt
import com.gegaremant.truenasmobile.data.models.Vm
import com.gegaremant.truenasmobile.ui.Screen
import com.gegaremant.truenasmobile.ui.components.ToastManager
import com.gegaremant.truenasmobile.ui.utils.AppCache
import kotlinx.coroutines.FlowPreview
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.debounce
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.launch

/**
 * Defines all types of searchable items in the app.
 * Navigation: the [route] property tells the navigation handler where to go.
 * Data holders: relevant data is carried in the result for pre-loading before navigation.
 */
sealed class SearchResult {
    abstract val id: String
    abstract val title: String
    abstract val subtitle: String
    abstract val category: SearchCategory
    abstract val relevanceScore: Float
    /** The base route for navigation (without arguments). Arguments are carried in the result itself. */
    abstract val route: String

    // ── Storage ──────────────────────────────────────────────
    data class PoolResult(
        override val id: String,
        override val title: String,
        override val subtitle: String,
        override val relevanceScore: Float,
        val pool: System.Pool
    ) : SearchResult() {
        override val category = SearchCategory.STORAGE
        override val route = Screen.PoolDetails.route
    }

    data class DiskResult(
        override val id: String,
        override val title: String,
        override val subtitle: String,
        override val relevanceScore: Float,
        val disk: System.DiskDetails
    ) : SearchResult() {
        override val category = SearchCategory.STORAGE
        override val route = Screen.DiskInfo.route
    }

    // ── Services ─────────────────────────────────────────────
    data class ServiceResult(
        override val id: String,
        override val title: String,
        override val subtitle: String,
        override val relevanceScore: Float,
        val service: System.ServiceQueryResponse
    ) : SearchResult() {
        override val category = SearchCategory.SERVICES
        override val route = Screen.ServicesScreen.route
    }

    // ── Shares ───────────────────────────────────────────────
    data class ShareResult(
        override val id: String,
        override val title: String,
        override val subtitle: String,
        override val relevanceScore: Float,
        val share: Shares.SmbShare
    ) : SearchResult() {
        override val category = SearchCategory.SHARES
        override val route = Screen.ShareInfo.route
    }

    // ── System Info ──────────────────────────────────────────
    data class SystemInfoResult(
        override val id: String,
        override val title: String,
        override val subtitle: String,
        override val relevanceScore: Float,
        val info: String,
        override val route: String = Screen.SystemInformationScreen.route
    ) : SearchResult() {
        override val category = SearchCategory.SYSTEM
    }

    // ── Actions ──────────────────────────────────────────────
    data class ActionResult(
        override val id: String,
        override val title: String,
        override val subtitle: String,
        override val relevanceScore: Float,
        val action: AppAction,
        override val route: String = ""  // actions don't navigate to a screen
    ) : SearchResult() {
        override val category = SearchCategory.ACTIONS
    }

    // ── Navigation / Screen shortcuts ────────────────────────
    data class NavigationResult(
        override val id: String,
        override val title: String,
        override val subtitle: String,
        override val relevanceScore: Float,
        val destinationRoute: String,
        /** Optional data setter lambda — called before navigation to pre-load any needed data. */
        val preNavigate: (suspend (TrueNASApiManager) -> Unit)? = null
    ) : SearchResult() {
        override val category = SearchCategory.NAVIGATION
        override val route = destinationRoute
    }

    // ── Installed Apps ───────────────────────────────────────
    data class InstalledAppResult(
        override val id: String,
        override val title: String,
        override val subtitle: String,
        override val relevanceScore: Float,
        val app: Apps.AppQueryResponse
    ) : SearchResult() {
        override val category = SearchCategory.APPS
        override val route = Screen.AppDetailsScreen.route
    }

    // ── Marketplace / Catalog Apps ───────────────────────────
    data class MarketplaceAppResult(
        override val id: String,
        override val title: String,
        override val subtitle: String,
        override val relevanceScore: Float,
        val app: Apps.AppAvailableItem
    ) : SearchResult() {
        override val category = SearchCategory.MARKETPLACE
        override val route = Screen.MarketplaceAppDetails.route
    }

    // ── Containers ───────────────────────────────────────────
    data class ContainerResult(
        override val id: String,
        override val title: String,
        override val subtitle: String,
        override val relevanceScore: Float,
        val container: Virt.ContainerResponse
    ) : SearchResult() {
        override val category = SearchCategory.CONTAINERS
        override val route = Screen.ContainerInfo.route
    }

    // ── VMs ──────────────────────────────────────────────────
    data class VmResult(
        override val id: String,
        override val title: String,
        override val subtitle: String,
        override val relevanceScore: Float,
        val vm: Vm.VmQueryResponse
    ) : SearchResult() {
        override val category = SearchCategory.VMS
        override val route = Screen.VmDetails.route
    }

    // ── Instance Settings ────────────────────────────────────
    data class InstanceSettingsResult(
        override val id: String,
        override val title: String,
        override val subtitle: String,
        override val relevanceScore: Float,
        override val route: String
    ) : SearchResult() {
        override val category = SearchCategory.INSTANCE_SETTINGS
    }
}

enum class SearchCategory(@StringRes val labelRes: Int) {
    ALL(R.string.search_category_all),
    NAVIGATION(R.string.search_category_screens),
    STORAGE(R.string.search_category_storage),
    SERVICES(R.string.search_category_services),
    SHARES(R.string.search_category_shares),
    SYSTEM(R.string.search_category_system),
    ACTIONS(R.string.search_category_actions),
    APPS(R.string.search_category_apps),
    MARKETPLACE(R.string.search_category_marketplace),
    CONTAINERS(R.string.search_category_containers),
    VMS(R.string.search_category_vms),
    INSTANCE_SETTINGS(R.string.search_category_instance_settings)
}

enum class AppAction {
    SHUTDOWN,
    RESTART,
    REFRESH_DATA
}

data class SearchState(
    val query: String = "",
    val results: List<SearchResult> = emptyList(),
    val isSearching: Boolean = false,
    val selectedCategory: SearchCategory = SearchCategory.ALL,
    val recentSearches: List<String> = emptyList()
)

@Suppress("UNCHECKED_CAST")
class SearchViewModelFactory(private val apiManager: TrueNASApiManager) :
    ViewModelProvider.Factory {
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(SearchViewModel::class.java)) {
            return SearchViewModel(apiManager) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }
}

/**
 * Comprehensive app-wide search ViewModel.
 *
 * Searches across:
 *  1. Static screen/subsection registry (instant, always available)
 *  2. Cached data from AppCache: pools, disks, shares, installed apps, containers, VMs
 *  3. Marketplace API (debounced remote queries)
 */
@OptIn(FlowPreview::class)
class SearchViewModel(
    private val apiManager: TrueNASApiManager
) : ViewModel() {

    private val _searchState = MutableStateFlow(SearchState())
    val searchState: StateFlow<SearchState> = _searchState.asStateFlow()

    private val _searchQuery = MutableStateFlow("")

    // Whether the marketplace catalogue has been consulted at least once. The
    // list itself is read straight from AppCache on every search: a snapshot
    // taken here went stale for the lifetime of this ViewModel, and the only
    // method that could refresh it had no callers.
    private var marketplaceLoaded = false

    init {
        // Debounce local search
        viewModelScope.launch {
            _searchQuery
                .debounce(300)
                .distinctUntilChanged()
                .collect { query ->
                    performSearch(query)
                }
        }
        marketplaceLoaded = AppCache.cachedMarketplaceApps.value.isNotEmpty()
    }

    private fun ensureMarketplaceLoaded() {
        if (marketplaceLoaded) return
        viewModelScope.launch {
            try {
                val result = apiManager.apps.queryMarketplaceAvailableItems()
                if (result is ApiResult.Success) {
                    // Into the shared cache, not a local field: the marketplace
                    // screen reads the same list and this was a second copy of
                    // app.available per session.
                    AppCache.updateMarketplaceApps(result.data)
                    marketplaceLoaded = true
                }
            } catch (_: Exception) { /* keep going */ }
        }
    }

    fun updateSearchQuery(query: String) {
        _searchQuery.value = query
        _searchState.value = _searchState.value.copy(
            query = query,
            isSearching = query.isNotEmpty()
        )
    }

    fun selectCategory(category: SearchCategory) {
        _searchState.value = _searchState.value.copy(selectedCategory = category)
        performSearch(_searchQuery.value)
    }

    fun clearSearch() {
        _searchQuery.value = ""
        _searchState.value = _searchState.value.copy(
            query = "",
            results = emptyList(),
            isSearching = false
        )
    }

    fun addToRecentSearches(query: String) {
        if (query.isBlank()) return
        val current = _searchState.value.recentSearches.toMutableList()
        current.remove(query)
        current.add(0, query)
        _searchState.value = _searchState.value.copy(
            recentSearches = current.take(10)
        )
    }

    fun removeRecentSearch(query: String) {
        val current = _searchState.value.recentSearches.toMutableList()
        current.remove(query)
        _searchState.value = _searchState.value.copy(recentSearches = current)
    }

    fun clearRecentSearches() {
        _searchState.value = _searchState.value.copy(recentSearches = emptyList())
    }

    // ═══════════════════════════════════════════════════════════
    //  SEARCH ENGINE
    // ═══════════════════════════════════════════════════════════

    private fun performSearch(query: String) {
        if (query.isBlank()) {
            _searchState.value = _searchState.value.copy(
                results = emptyList(),
                isSearching = false
            )
            return
        }

        viewModelScope.launch {
            val results = mutableListOf<SearchResult>()
            val lowerQuery = query.lowercase().trim()
            val selectedCategory = _searchState.value.selectedCategory

            // ── 1. Static Screen / Subsection Registry ────────
            if (selectedCategory == SearchCategory.ALL || selectedCategory == SearchCategory.NAVIGATION) {
                results.addAll(searchScreenRegistry(lowerQuery))
            }

            // ── 1.5 Instance Settings registry ────────────────
            if (selectedCategory == SearchCategory.ALL || selectedCategory == SearchCategory.INSTANCE_SETTINGS) {
                results.addAll(searchInstanceSettingsRegistry(lowerQuery))
            }

            // ── 2. Pools ──────────────────────────────────────
            if (selectedCategory == SearchCategory.ALL || selectedCategory == SearchCategory.STORAGE) {
                val pools = AppCache.cachedPools.value
                pools.forEach { pool ->
                    val relevance = calculateRelevance(lowerQuery, pool.name, pool.status_detail ?: "")
                    if (relevance > 0) {
                        results.add(
                            SearchResult.PoolResult(
                                id = "pool_${pool.id}",
                                title = pool.name,
                                subtitle = ToastManager.resolveString(R.string.search_pool_fmt, formatBytes(pool.size ?: 0)),
                                relevanceScore = relevance,
                                pool = pool
                            )
                        )
                    }
                }

                // Disks
                val disks = AppCache.cachedDisks.value
                disks.forEach { disk ->
                    val relevance = calculateRelevance(lowerQuery, disk.name, disk.model ?: "", disk.serial)
                    if (relevance > 0) {
                        results.add(
                            SearchResult.DiskResult(
                                id = "disk_${disk.name}",
                                title = disk.name,
                                subtitle = ToastManager.resolveString(R.string.search_disk_fmt, disk.model ?: ToastManager.resolveString(R.string.common_unknown)),
                                relevanceScore = relevance,
                                disk = disk
                            )
                        )
                    }
                }

                // Shares
                if (selectedCategory == SearchCategory.ALL || selectedCategory == SearchCategory.SHARES) {
                    AppCache.cachedSmbShares.value.forEach { share ->
                        val relevance = calculateRelevance(lowerQuery, share.name, share.path, share.comment ?: "")
                        if (relevance > 0) {
                            results.add(
                                SearchResult.ShareResult(
                                    id = "share_${share.id}",
                                    title = share.name,
                                    subtitle = ToastManager.resolveString(R.string.search_share_fmt, share.path),
                                    relevanceScore = relevance,
                                    share = share
                                )
                            )
                        }
                    }
                }
            }

            // ── 3. System Info ───────────────────────────────
            if (selectedCategory == SearchCategory.ALL || selectedCategory == SearchCategory.SYSTEM) {
                AppCache.cachedSystemInfo.value?.let { info ->
                    listOf(
                        "hostname" to info.hostname,
                        "version" to info.version,
                        "uptime" to info.uptime,
                        "cpu cores" to ToastManager.resolveString(R.string.search_cores_fmt, info.cores.toInt())
                    ).forEach { (key, value) ->
                        val relevance = calculateRelevance(lowerQuery, key, value)
                        if (relevance > 0) {
                            results.add(
                                SearchResult.SystemInfoResult(
                                    id = "system_$key",
                                    title = key.replaceFirstChar { it.uppercase() },
                                    subtitle = ToastManager.resolveString(R.string.search_system_fmt, value),
                                    relevanceScore = relevance,
                                    info = value
                                )
                            )
                        }
                    }
                }
            }

            // ── 3.5 Services ─────────────────────────────
            if (selectedCategory == SearchCategory.ALL || selectedCategory == SearchCategory.SERVICES) {
                AppCache.cachedServices.value.forEach { svc ->
                    val relevance = calculateRelevance(lowerQuery, svc.service, svc.state)
                    if (relevance > 0) {
                        results.add(
                            SearchResult.ServiceResult(
                                id = "svc_${svc.id}",
                                title = svc.service.replaceFirstChar { it.uppercase() },
                                subtitle = ToastManager.resolveString(
                                    R.string.search_service_fmt,
                                    ToastManager.resolveString(if (svc.enable) R.string.search_state_running else R.string.search_state_stopped)
                                ),
                                relevanceScore = relevance,
                                service = svc
                            )
                        )
                    }
                }
            }

            // ── 4. Installed Apps ─────────────────────────────
            if (selectedCategory == SearchCategory.ALL || selectedCategory == SearchCategory.APPS) {
                val installedApps = AppCache.cachedApps.value
                installedApps.forEach { app ->
                    val metadata = app.metadata
                    val relevance = calculateRelevance(
                        lowerQuery,
                        app.name,
                        metadata?.title ?: "",
                        metadata?.description ?: "",
                        metadata?.categories?.joinToString(" ") ?: "",
                        metadata?.keywords?.joinToString(" ") ?: ""
                    )
                    if (relevance > 0) {
                        results.add(
                            SearchResult.InstalledAppResult(
                                id = "installed_${app.id}",
                                title = metadata?.title ?: app.name,
                                subtitle = ToastManager.resolveString(
                                    R.string.search_installed_app_fmt,
                                    app.version ?: ToastManager.resolveString(R.string.search_unknown_value)
                                ),
                                relevanceScore = relevance,
                                app = app
                            )
                        )
                    }
                }
            }

            // ── 5. Marketplace Apps ───────────────────────────
            if (selectedCategory == SearchCategory.ALL || selectedCategory == SearchCategory.MARKETPLACE) {
                ensureMarketplaceLoaded()
                AppCache.cachedMarketplaceApps.value.forEach { app ->
                    val relevance = calculateRelevance(
                        lowerQuery,
                        app.name,
                        app.title,
                        app.description,
                        app.tags?.joinToString(" ") ?: "",
                        app.categories?.joinToString(" ") ?: ""
                    )
                    if (relevance > 0) {
                        val installedTag = if (app.installed) ToastManager.resolveString(R.string.search_marketplace_tag_installed) else ""
                        results.add(
                            SearchResult.MarketplaceAppResult(
                                id = "marketplace_${app.name}",
                                title = app.title.ifBlank { app.name },
                                subtitle = ToastManager.resolveString(
                                    R.string.search_marketplace_fmt,
                                    ToastManager.resolveString(R.string.marketplace_title) +
                                        (if (installedTag.isEmpty()) "" else " • $installedTag"),
                                    app.categories?.firstOrNull()?.replaceFirstChar { it.uppercase() }
                                        ?: ToastManager.resolveString(R.string.search_app_fallback)
                                ),
                                relevanceScore = relevance,
                                app = app
                            )
                        )
                    }
                }
            }

            // ── 6. Containers ─────────────────────────────────
            if (selectedCategory == SearchCategory.ALL || selectedCategory == SearchCategory.CONTAINERS) {
                AppCache.cachedContainers.value.forEach { container ->
                    val relevance = calculateRelevance(
                        lowerQuery,
                        container.name,
                        container.status.name,
                        container.image.os ?: "",
                        container.image.release ?: ""
                    )
                    if (relevance > 0) {
                        val statusEmoji = when (container.status) {
                            Virt.Status.RUNNING -> " " + ToastManager.resolveString(R.string.search_state_running)
                            Virt.Status.STOPPED -> " " + ToastManager.resolveString(R.string.search_state_stopped)
                            else -> " • ${container.status.name}"
                        }
                        results.add(
                            SearchResult.ContainerResult(
                                id = "container_${container.id}",
                                title = container.name,
                                subtitle = ToastManager.resolveString(R.string.search_container_word) + statusEmoji,
                                relevanceScore = relevance,
                                container = container
                            )
                        )
                    }
                }
            }

            // ── 7. VMs ────────────────────────────────────────
            if (selectedCategory == SearchCategory.ALL || selectedCategory == SearchCategory.VMS) {
                AppCache.cachedVms.value.forEach { vm ->
                    val relevance = calculateRelevance(
                        lowerQuery,
                        vm.name,
                        vm.description,
                        vm.status.state,
                        vm.arch_type ?: "",
                        "${vm.vcpus} vcpus",
                        "${vm.memory}MB"
                    )
                    if (relevance > 0) {
                        val state = vm.status.state.replaceFirstChar { it.uppercase() }
                        results.add(
                            SearchResult.VmResult(
                                id = "vm_${vm.id}",
                                title = vm.name,
                                subtitle = ToastManager.resolveString(
                                    R.string.search_vm_fmt,
                                    state,
                                    vm.vcpus.toString(),
                                    vm.memory.toString()
                                ),
                                relevanceScore = relevance,
                                vm = vm
                            )
                        )
                    }
                }
            }

            // ── 8. Actions ────────────────────────────────────
            // NOTE: system actions (shutdown / restart / refresh-all) are
            // intentionally NOT exposed through search yet. They were returned
            // as results but nothing ever handled the tap, and triggering a
            // shutdown from a search overlay with no confirmation dialog is
            // dangerous. They stay available from the Home header, which does
            // ask for confirmation. See TODO.md.

            // Sort by relevance descending
            val sortedResults = results.sortedByDescending { it.relevanceScore }

            _searchState.value = _searchState.value.copy(
                results = sortedResults,
                isSearching = false
            )
        }
    }

    // ═══════════════════════════════════════════════════════════
    //  SCREEN REGISTRY — static list of every navigable screen
    // ═══════════════════════════════════════════════════════════

    private data class ScreenEntry(
        val id: String,
        @StringRes val titleRes: Int,
        @StringRes val subtitleRes: Int,
        val route: String,
        val keywords: List<String>
    ) {
        val title: String get() = ToastManager.resolveString(titleRes)
        val subtitle: String get() = ToastManager.resolveString(subtitleRes)
    }

    private val screenRegistry: List<ScreenEntry> = listOf(
        // Top-level tabs
        ScreenEntry("nav_home", R.string.search_nav_home_title, R.string.search_nav_home_subtitle, Screen.Home.route,
            listOf("dashboard", "home", "overview", "main")),
        ScreenEntry("nav_apps", R.string.search_nav_apps_title, R.string.search_nav_apps_subtitle, Screen.Apps.route,
            listOf("apps", "applications", "installed")),
        ScreenEntry("nav_containers", R.string.search_nav_containers_title, R.string.search_nav_containers_subtitle, Screen.Containers.route,
            listOf("containers", "docker", "lxc", "container")),
        ScreenEntry("nav_vms", R.string.search_nav_vms_title, R.string.search_nav_vms_subtitle, Screen.Vms.route,
            listOf("vms", "virtual machines", "kvm", "vm")),

        // Marketplace
        ScreenEntry("nav_marketplace", R.string.search_nav_marketplace_title, R.string.search_nav_marketplace_subtitle, Screen.Marketplace.route,
            listOf("marketplace", "catalog", "discover", "store", "install")),

        // Settings & sub-screens
        ScreenEntry("nav_settings", R.string.search_nav_settings_title, R.string.search_nav_settings_subtitle, Screen.Settings.route,
            listOf("settings", "preferences", "config")),
        ScreenEntry("nav_about", R.string.search_nav_about_title, R.string.search_nav_about_subtitle, Screen.About.route,
            listOf("about", "version", "credits")),
        ScreenEntry("nav_theme", R.string.search_nav_theme_title, R.string.search_nav_theme_subtitle, Screen.Theme.route,
            listOf("theme", "dark mode", "light mode", "appearance", "colors")),
        ScreenEntry("nav_licenses", R.string.search_nav_licenses_title, R.string.search_nav_licenses_subtitle, Screen.Licenses.route,
            listOf("licenses", "open source", "oss")),

        // Home sub-screens
        ScreenEntry("nav_pools", R.string.search_nav_pools_title, R.string.search_nav_pools_subtitle, Screen.Home.route,
            listOf("pools", "storage", "zfs", "pool")),
        ScreenEntry("nav_disks", R.string.search_nav_disks_title, R.string.search_nav_disks_subtitle, Screen.DiskInfo.route,
            listOf("disks", "drives", "hard drives", "nvme", "ssd", "hdd")),
        ScreenEntry("nav_shares", R.string.search_nav_shares_title, R.string.search_nav_shares_subtitle, Screen.Home.route,
            listOf("shares", "smb", "nfs", "file sharing", "samba")),
        ScreenEntry("nav_performance", R.string.search_nav_performance_title, R.string.search_nav_performance_subtitle, Screen.Performance.route,
            listOf("performance", "cpu", "memory", "temperature", "metrics", "stats")),
        ScreenEntry("nav_system_update", R.string.search_nav_system_update_title, R.string.search_nav_system_update_subtitle, Screen.SystemUpdateScreen.route,
            listOf("update", "upgrade", "version", "system update")),
        ScreenEntry("nav_system_info", R.string.search_nav_system_info_title, R.string.search_nav_system_info_subtitle, Screen.SystemInformationScreen.route,
            listOf("system info", "hostname", "version", "uptime", "platform")),
        ScreenEntry("nav_software_info", R.string.search_nav_software_info_title, R.string.search_nav_software_info_subtitle, Screen.SoftwareInformationScreen.route,
            listOf("software", "license", "os")),
        ScreenEntry("nav_hardware_info", R.string.search_nav_hardware_info_title, R.string.search_nav_hardware_info_subtitle, Screen.HardwareInformationScreen.route,
            listOf("hardware", "cpu model", "memory", "physical", "serial")),

        // Instance config sub-screens
        ScreenEntry("nav_instance_config", R.string.search_nav_instance_config_title, R.string.search_nav_instance_config_subtitle, Screen.InstanceConfigScreen.route,
            listOf("instance", "configuration", "truenas settings", "server config")),
        ScreenEntry("nav_services", R.string.search_nav_services_title, R.string.search_nav_services_subtitle, Screen.ServicesScreen.route,
            listOf("services", "ssh", "nfs", "smb", "ftp", "rsync", "iscsi")),
        ScreenEntry("nav_users", R.string.search_nav_users_title, R.string.search_nav_users_subtitle, Screen.UserListScreen.route,
            listOf("users", "accounts", "user list")),
        ScreenEntry("nav_api_keys", R.string.search_nav_api_keys_title, R.string.search_nav_api_keys_subtitle, Screen.ApiKeyListScreen.route,
            listOf("api keys", "tokens", "bearer")),
        ScreenEntry("nav_network", R.string.search_nav_network_title, R.string.search_nav_network_subtitle, Screen.NetworkScreen.route,
            listOf("network", "interfaces", "ip", "dns", "gateway")),
        ScreenEntry("nav_boot", R.string.search_nav_boot_title, R.string.search_nav_boot_subtitle, Screen.BootScreen.route,
            listOf("boot", "boot environments", "boot pool", "startup")),
        ScreenEntry("nav_alerts", R.string.search_nav_alerts_title, R.string.search_nav_alerts_subtitle, Screen.AlertServicesList.route,
            listOf("alerts", "notifications", "alert services", "email")),
        ScreenEntry("nav_audit", R.string.search_nav_audit_title, R.string.search_nav_audit_subtitle, Screen.AuditConfigScreen.route,
            listOf("audit", "logs", "audit logs")),
        ScreenEntry("nav_general_settings", R.string.search_nav_general_settings_title, R.string.search_nav_general_settings_subtitle, Screen.GeneralSystemSettingsScreen.route,
            listOf("general settings", "gui", "https", "timezone", "language")),
        ScreenEntry("nav_advanced_settings", R.string.search_nav_advanced_settings_title, R.string.search_nav_advanced_settings_subtitle, Screen.AdvancedSystemSettingsScreen.route,
            listOf("advanced settings", "sysctl", "kernel", "advanced")),
        ScreenEntry("nav_truenas_connect", R.string.search_nav_truenas_connect_title, R.string.search_nav_truenas_connect_subtitle, Screen.TrueNasConnectScreen.route,
            listOf("truenas connect", "cloud", "ixsystems")),
        ScreenEntry("nav_truecommand", R.string.search_nav_truecommand_title, R.string.search_nav_truecommand_subtitle, Screen.TrueCommandScreen.route,
            listOf("truecommand", "fleet", "management")),

        // Account
        ScreenEntry("nav_account", R.string.search_nav_account_title, R.string.search_nav_account_subtitle, Screen.AccountSwitcher.route,
            listOf("account", "login", "logout", "switch", "profile")),
        ScreenEntry("nav_change_password", R.string.search_nav_change_password_title, R.string.search_nav_change_password_subtitle, Screen.ChangePassword.route,
            listOf("change password", "password", "credentials")),
    )

    // ── Instance Settings registry — every section reachable from InstanceConfigScreen ──
    private val instanceSettingsRegistry: List<ScreenEntry> = listOf(
        // Core configuration
        ScreenEntry("is_general", R.string.search_is_general_title, R.string.search_is_general_subtitle,
            Screen.GeneralSystemSettingsScreen.route,
            listOf("general settings", "hostname", "timezone", "gui", "https", "language", "console", "motd")),
        ScreenEntry("is_advanced", R.string.search_is_advanced_title, R.string.search_is_advanced_subtitle,
            Screen.AdvancedSystemSettingsScreen.route,
            listOf("advanced settings", "sysctl", "kernel", "tunables", "developer", "crash reporting")),
        ScreenEntry("is_network", R.string.search_is_network_title, R.string.search_is_network_subtitle,
            Screen.NetworkScreen.route,
            listOf("network", "interfaces", "ip", "dns", "gateway", "routes", "link")),
        ScreenEntry("is_boot", R.string.search_is_boot_title, R.string.search_is_boot_subtitle,
            Screen.BootScreen.route,
            listOf("boot", "boot environments", "boot pool", "startup", "recovery")),
        ScreenEntry("is_services", R.string.search_is_services_title, R.string.search_is_services_subtitle,
            Screen.ServicesScreen.route,
            listOf("services", "ssh", "nfs", "smb", "ftp", "rsync", "iscsi", "snmp")),
        ScreenEntry("is_users", R.string.search_is_users_title, R.string.search_is_users_subtitle,
            Screen.UserListScreen.route,
            listOf("users", "accounts", "local users", "user list")),
        ScreenEntry("is_api_keys", R.string.search_is_api_keys_title, R.string.search_is_api_keys_subtitle,
            Screen.ApiKeyListScreen.route,
            listOf("api keys", "keys", "tokens", "bearer", "access")),

        // Monitoring & compliance
        ScreenEntry("is_alerts", R.string.search_is_alerts_title, R.string.search_is_alerts_subtitle,
            Screen.AlertServicesList.route,
            listOf("alerts", "alert services", "notifications", "email", "webhook")),
        ScreenEntry("is_audit_config", R.string.search_is_audit_config_title, R.string.search_is_audit_config_subtitle,
            Screen.AuditConfigScreen.route,
            listOf("audit", "audit config", "retention", "logging", "zfs dataset")),
        ScreenEntry("is_audit_logs", R.string.search_is_audit_logs_title, R.string.search_is_audit_logs_subtitle,
            Screen.AuditLogsScreen.route,
            listOf("audit logs", "logs", "audit entries", "export")),

        // System information
        ScreenEntry("is_truenas_connect", R.string.search_is_truenas_connect_title, R.string.search_is_truenas_connect_subtitle,
            Screen.TrueNasConnectScreen.route,
            listOf("truenas connect", "cloud", "connect", "ixsystems")),
        ScreenEntry("is_truecommand", R.string.search_is_truecommand_title, R.string.search_is_truecommand_subtitle,
            Screen.TrueCommandScreen.route,
            listOf("truecommand", "fleet", "management", "cluster")),
        ScreenEntry("is_system_info", R.string.search_is_system_info_title, R.string.search_is_system_info_subtitle,
            Screen.SystemInformationScreen.route,
            listOf("system information", "system info", "version", "platform", "hardware", "uuid")),
    )

    private fun searchInstanceSettingsRegistry(lowerQuery: String): List<SearchResult.InstanceSettingsResult> {
        return instanceSettingsRegistry.mapNotNull { entry ->
            val relevance = calculateRelevance(lowerQuery, entry.title, entry.subtitle, *entry.keywords.toTypedArray())
            if (relevance > 0) {
                SearchResult.InstanceSettingsResult(
                    id = entry.id,
                    title = entry.title,
                    subtitle = ToastManager.resolveString(R.string.search_instance_setting_prefix, entry.subtitle),
                    relevanceScore = relevance + 0.5f,
                    route = entry.route
                )
            } else null
        }
    }

    private fun searchScreenRegistry(lowerQuery: String): List<SearchResult.NavigationResult> {
        return screenRegistry.mapNotNull { entry ->
            val relevance = calculateRelevance(lowerQuery, entry.title, entry.subtitle, *entry.keywords.toTypedArray())
            if (relevance > 0) {
                SearchResult.NavigationResult(
                    id = entry.id,
                    title = entry.title,
                    subtitle = entry.subtitle,
                    relevanceScore = relevance + 0.5f, // slight boost for screen nav
                    destinationRoute = entry.route
                )
            } else null
        }
    }

    // ═══════════════════════════════════════════════════════════
    //  RELEVANCE SCORING
    // ═══════════════════════════════════════════════════════════

    private fun calculateRelevance(query: String, vararg fields: String): Float {
        var score = 0f

        fields.forEach { field ->
            val lowerField = field.lowercase()
            when {
                lowerField == query -> score += 10f
                lowerField.startsWith(query) -> score += 7f
                lowerField.split(" ", "-", "_").any { it == query } -> score += 5f
                lowerField.contains(query) -> score += 3f
                isFuzzyMatch(query, lowerField) -> score += 1f
            }
        }

        return score
    }

    private fun isFuzzyMatch(query: String, text: String): Boolean {
        var queryIndex = 0
        for (char in text) {
            if (queryIndex < query.length && char == query[queryIndex]) {
                queryIndex++
            }
        }
        return queryIndex == query.length
    }

    private fun formatBytes(bytes: Long): String {
        val units = arrayOf("B", "KB", "MB", "GB", "TB")
        var size = bytes.toDouble()
        var unitIndex = 0
        while (size >= 1024 && unitIndex < units.size - 1) {
            size /= 1024.0
            unitIndex++
        }
        return "%.1f %s".format(size, units[unitIndex])
    }
}
