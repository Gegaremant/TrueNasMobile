package com.gegaremant.truenasmobile.ui

import android.content.res.Configuration
import androidx.compose.animation.AnimatedContentTransitionScope
import androidx.compose.animation.Crossfade
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.background
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.material3.OutlinedButton
import androidx.compose.foundation.layout.Column
import androidx.compose.material.icons.filled.Info
import androidx.compose.ui.Alignment
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Apps
import androidx.compose.material.icons.outlined.Settings
import androidx.compose.material.icons.filled.Settings
import com.gegaremant.truenasmobile.data.helpers.PersonalizationManager
import com.gegaremant.truenasmobile.data.helpers.NavbarDestination
import androidx.compose.material.icons.filled.Checklist
import androidx.compose.material.icons.filled.Computer
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Inventory
import androidx.compose.material.icons.filled.ShowChart
import androidx.compose.material.icons.filled.Storage
import androidx.compose.material.icons.filled.Storefront
import androidx.compose.material.icons.filled.SystemUpdateAlt
import androidx.compose.material.icons.filled.Tune
import androidx.compose.material.icons.outlined.Apps
import androidx.compose.material.icons.outlined.Checklist
import androidx.compose.material.icons.outlined.Computer
import androidx.compose.material.icons.outlined.Home
import androidx.compose.material.icons.outlined.Inventory2
import androidx.compose.material.icons.outlined.ShowChart
import androidx.compose.material.icons.outlined.Storage
import androidx.compose.material.icons.outlined.Storefront
import androidx.compose.material.icons.outlined.SystemUpdateAlt
import androidx.compose.material.icons.outlined.Tune
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.NavigationRail
import androidx.compose.material3.NavigationRailItem
import androidx.compose.material3.NavigationRailItemDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavController
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.NavHostController
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.gegaremant.truenasmobile.MainViewModel
import com.gegaremant.truenasmobile.R
import com.gegaremant.truenasmobile.data.api.TrueNASApiManager
import com.gegaremant.truenasmobile.data.models.System
import com.gegaremant.truenasmobile.data.models.canUpgradeNow
import com.gegaremant.truenasmobile.ui.components.LoadingScreen
import com.gegaremant.truenasmobile.ui.homepage.HomeScreen
import com.gegaremant.truenasmobile.ui.homepage.HomeUiState
import com.gegaremant.truenasmobile.ui.homepage.HomeViewModel
import com.gegaremant.truenasmobile.ui.homepage.StorageScreen
import com.gegaremant.truenasmobile.ui.homepage.TasksScreen
import com.gegaremant.truenasmobile.ui.homepage.dataset.DatasetExplorerScreen
import com.gegaremant.truenasmobile.ui.homepage.details.DiskInfoScreen
import com.gegaremant.truenasmobile.ui.homepage.details.PerformanceScreen
import com.gegaremant.truenasmobile.ui.homepage.details.ShareInfoScreen
import com.gegaremant.truenasmobile.ui.homepage.instancesettings.InstanceConfigScreen
import com.gegaremant.truenasmobile.ui.homepage.instancesettings.advanced.AdvancedSystemSettingsEditScreen
import com.gegaremant.truenasmobile.ui.homepage.instancesettings.advanced.AdvancedSystemSettingsScreen
import com.gegaremant.truenasmobile.ui.homepage.instancesettings.alertservice.AlertClassesConfigScreen
import com.gegaremant.truenasmobile.ui.homepage.instancesettings.alertservice.AlertServiceCreateScreen
import com.gegaremant.truenasmobile.ui.homepage.instancesettings.alertservice.AlertServiceDetailScreen
import com.gegaremant.truenasmobile.ui.homepage.instancesettings.alertservice.AlertServicesListScreen
import com.gegaremant.truenasmobile.ui.homepage.instancesettings.apikeys.ApiKeyCreateScreen
import com.gegaremant.truenasmobile.ui.homepage.instancesettings.apikeys.ApiKeyDetailScreen
import com.gegaremant.truenasmobile.ui.homepage.instancesettings.apikeys.ApiKeyListScreen
import com.gegaremant.truenasmobile.ui.homepage.instancesettings.audit.AuditConfigScreen
import com.gegaremant.truenasmobile.ui.homepage.instancesettings.audit.AuditConfigViewModel
import com.gegaremant.truenasmobile.ui.homepage.instancesettings.audit.AuditLogsScreen
import com.gegaremant.truenasmobile.ui.homepage.instancesettings.audit.AuditLogsViewModel
import com.gegaremant.truenasmobile.ui.homepage.instancesettings.boot.BootEnvironmentDetailScreen
import com.gegaremant.truenasmobile.ui.homepage.instancesettings.boot.BootEnvironmentsScreen
import com.gegaremant.truenasmobile.ui.homepage.instancesettings.boot.BootPoolScreen
import com.gegaremant.truenasmobile.ui.homepage.instancesettings.boot.BootScreen
import com.gegaremant.truenasmobile.ui.homepage.instancesettings.general.GeneralSystemSettingsEditScreen
import com.gegaremant.truenasmobile.ui.homepage.instancesettings.appimages.AppImageManagementScreen
import com.gegaremant.truenasmobile.ui.homepage.instancesettings.appimages.DockerImageListScreen
import com.gegaremant.truenasmobile.ui.homepage.instancesettings.appimages.IxVolumeListScreen
import com.gegaremant.truenasmobile.ui.homepage.instancesettings.general.GeneralSystemSettingsScreen
import com.gegaremant.truenasmobile.ui.homepage.instancesettings.network.NetworkEditScreen
import com.gegaremant.truenasmobile.ui.homepage.instancesettings.network.NetworkScreen
import com.gegaremant.truenasmobile.ui.homepage.instancesettings.service.ServicesScreen
import com.gegaremant.truenasmobile.ui.homepage.instancesettings.systeminformation.HardwareInformationScreen
import com.gegaremant.truenasmobile.ui.homepage.instancesettings.systeminformation.SoftwareInformationScreen
import com.gegaremant.truenasmobile.ui.homepage.instancesettings.systeminformation.SystemInformationScreen
import com.gegaremant.truenasmobile.ui.homepage.instancesettings.systeminformation.TrueCommandScreen
import com.gegaremant.truenasmobile.ui.homepage.instancesettings.systeminformation.TrueNasConnectScreen
import com.gegaremant.truenasmobile.ui.homepage.instancesettings.users.LocalAdminSetupScreen
import com.gegaremant.truenasmobile.ui.homepage.instancesettings.users.UserCreateScreen
import com.gegaremant.truenasmobile.ui.homepage.instancesettings.users.UserDetailScreen
import com.gegaremant.truenasmobile.ui.homepage.instancesettings.users.UserListScreen
import com.gegaremant.truenasmobile.ui.homepage.pools.PoolDataHolder
import com.gegaremant.truenasmobile.ui.homepage.pools.PoolDetailsScreen
import com.gegaremant.truenasmobile.ui.homepage.update.SystemUpdateScreen
import com.gegaremant.truenasmobile.ui.services.apps.AppsScreen
import com.gegaremant.truenasmobile.ui.services.apps.AppsScreenViewModel
import com.gegaremant.truenasmobile.ui.services.apps.details.appdetails.AppConfigPageValues
import com.gegaremant.truenasmobile.ui.services.apps.details.appdetails.AppConfigScreen
import com.gegaremant.truenasmobile.ui.services.apps.details.appdetails.AppDataHolder
import com.gegaremant.truenasmobile.ui.services.apps.details.appdetails.AppAdvancedInfoScreen
import com.gegaremant.truenasmobile.ui.services.apps.details.appdetails.AppInfoScreen
import com.gegaremant.truenasmobile.ui.services.apps.details.marketplace.MarketplaceAppDetailsScreen
import com.gegaremant.truenasmobile.ui.services.apps.details.marketplace.MarketplaceAppInstallScreen
import com.gegaremant.truenasmobile.ui.services.apps.details.marketplace.MarketplaceScreen
import com.gegaremant.truenasmobile.ui.services.apps.details.rollback.RollbackVersionScreen
import com.gegaremant.truenasmobile.ui.services.apps.details.upgrade.UpgradeSummaryScreen
import com.gegaremant.truenasmobile.ui.services.containers.ContainersScreen
import com.gegaremant.truenasmobile.ui.services.containers.details.ContainerDataHolder
import com.gegaremant.truenasmobile.ui.services.containers.details.ContainerInfoScreen
import com.gegaremant.truenasmobile.ui.services.system.services.ServiceDetailScreen
import com.gegaremant.truenasmobile.ui.services.vm.VmsScreen
import com.gegaremant.truenasmobile.ui.services.vm.details.VmDataHolder
import com.gegaremant.truenasmobile.ui.services.vm.details.VmInfoScreen
import com.gegaremant.truenasmobile.ui.topbar.ExpressiveSearchAppBar
import com.gegaremant.truenasmobile.ui.topbar.SearchResultNavigation
import com.gegaremant.truenasmobile.ui.utils.AppCache


private data class NavItem(
    val destination: NavbarDestination,
    @androidx.annotation.StringRes val titleRes: Int,
    val selectedIcon: ImageVector,
    val unselectedIcon: ImageVector
)

/**
 * Icons for a bottom-bar destination. Labels are not chosen here - they come
 * from [NavbarDestination.titleRes] so the locale dictionaries stay the single
 * source of truth.
 */
private fun destinationToNavItem(destination: NavbarDestination): NavItem? {
    return when (destination) {
        NavbarDestination.HOME ->
            NavItem(destination, R.string.nav_statistics, Icons.Filled.Home, Icons.Outlined.Home)
        NavbarDestination.STORAGE ->
            NavItem(destination, R.string.nav_storage, Icons.Filled.Storage, Icons.Outlined.Storage)
        NavbarDestination.APPS ->
            NavItem(destination, R.string.nav_apps, Icons.Filled.Apps, Icons.Outlined.Apps)
        NavbarDestination.TASKS ->
            NavItem(destination, R.string.nav_tasks, Icons.Filled.Checklist, Icons.Outlined.Checklist)
        NavbarDestination.PERFORMANCE ->
            NavItem(destination, R.string.nav_graphs, Icons.Filled.ShowChart, Icons.Outlined.ShowChart)
        NavbarDestination.CONTAINERS ->
            NavItem(destination, R.string.nav_containers, Icons.Filled.Apps, Icons.Outlined.Apps)
        NavbarDestination.VMS ->
            NavItem(destination, R.string.nav_vms, Icons.Filled.Computer, Icons.Outlined.Computer)
        NavbarDestination.MARKETPLACE ->
            NavItem(destination, R.string.nav_marketplace, Icons.Filled.Storefront, Icons.Outlined.Storefront)
        NavbarDestination.INSTANCE_SETTINGS ->
            NavItem(destination, R.string.nav_instance_settings, Icons.Filled.Settings, Icons.Outlined.Settings)
        NavbarDestination.UPDATES ->
            NavItem(destination, R.string.nav_updates, Icons.Filled.SystemUpdateAlt, Icons.Outlined.SystemUpdateAlt)
    }
}

@Composable
fun MainScreen(
    manager: TrueNASApiManager,
    rootNavController: NavController,
    viewModel: MainViewModel
) {
    val navController = rememberNavController()
    val configuration = LocalConfiguration.current
    val isLandscape = configuration.orientation == Configuration.ORIENTATION_LANDSCAPE
    val personalization by PersonalizationManager.state.collectAsState()

    val navItems = remember(personalization.navbarDestinations) {
        personalization.navbarDestinations.mapNotNull { destinationToNavItem(it) }
    }
    val navRoutes = remember(navItems) { navItems.map { it.destination.route }.toSet() }
    // Compact mode drops the text labels; there is room for more destinations.
    val compactNav = personalization.compactNav

    val navBackStackEntry by navController.currentBackStackEntryAsState()
    val currentRoute = navBackStackEntry?.destination?.route
    val pendingNav by viewModel.pendingNavigation.collectAsState()
    LaunchedEffect(pendingNav) {
        val target = pendingNav
        when (target) {
            Screen.Apps.route,
            Screen.Marketplace.route,
            Screen.InstanceConfigScreen.route,
            Screen.SystemUpdateScreen.route -> navController.navigate(
                target
            ) {
                popUpTo(navController.graph.findStartDestination().id) { saveState = true }
                launchSingleTop = true
                restoreState = true
            }
            else -> return@LaunchedEffect
        }
        viewModel.clearPendingNavigation()
    }

    var showSearch by remember { mutableStateOf(false) }

    Box(modifier = Modifier.fillMaxSize()) {
        if (isLandscape) {
            Row {
                NavigationRail(
                    containerColor = MaterialTheme.colorScheme.surface,
                    modifier = Modifier.fillMaxHeight(),
                    header = {}
                ) {
                    navItems.forEach { item ->
                        val selected = currentRoute == item.destination.route
                        NavigationRailItem(
                            selected = selected,
                            onClick = { onNavClick(navController, item.destination.route) },
                            label = {
                                if (!compactNav) {
                                    Text(stringResource(item.titleRes), fontWeight = if (selected) FontWeight.Bold else FontWeight.Normal)
                                }
                            },
                            icon = {
                                Crossfade(targetState = selected, label = "iconFade") { isSelected ->
                                    Icon(
                                        if (isSelected) item.selectedIcon else item.unselectedIcon,
                                        stringResource(item.titleRes)
                                    )
                                }
                            },
                            colors = NavigationRailItemDefaults.colors(
                                indicatorColor = MaterialTheme.colorScheme.secondaryContainer,
                                selectedIconColor = MaterialTheme.colorScheme.onSecondaryContainer,
                                unselectedIconColor = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        )
                    }
                }
                TrueNasMobileNavGraph(
                    navController = navController,
                    manager = manager,
                    rootNavController = rootNavController,
                    onSearchClick = { showSearch = true },
                    modifier = Modifier.weight(1f)
                )
            }
        } else {
            Scaffold(
                bottomBar = {if (currentRoute in navRoutes){
                    run {
                        NavigationBar(
                            containerColor = MaterialTheme.colorScheme.surface,
                            tonalElevation = 8.dp
                        ) {
                            navItems.forEach { item ->
                                val selected = currentRoute == item.destination.route
                                NavigationBarItem(
                                    selected = selected,
                                    onClick = { onNavClick(navController, item.destination.route) },
                                    alwaysShowLabel = !compactNav,
                                    label = {
                                        Text(
                                            stringResource(item.titleRes),
                                            fontWeight = if (selected) FontWeight.Bold else FontWeight.Normal
                                        )
                                    },
                                    icon = {
                                        Crossfade(
                                            targetState = selected,
                                            label = "iconFade"
                                        ) { isSelected ->
                                            Icon(
                                                if (isSelected) item.selectedIcon else item.unselectedIcon,
                                                stringResource(item.titleRes)
                                            )
                                        }
                                    },
                                    colors = NavigationBarItemDefaults.colors(
                                        indicatorColor = MaterialTheme.colorScheme.secondaryContainer,
                                        selectedIconColor = MaterialTheme.colorScheme.onSecondaryContainer,
                                        unselectedIconColor = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                )
                            }
                        }
                    }
                }
                }
            ) { innerPadding ->
                TrueNasMobileNavGraph(
                    navController = navController,
                    manager = manager,
                    rootNavController = rootNavController,
                    onSearchClick = { showSearch = true },
                    modifier = Modifier.padding(innerPadding)
                )
            }
        }

        // Full-screen search overlay
        if (showSearch) {
            ExpressiveSearchAppBar(
                title = stringResource(R.string.search_cd),
                manager = manager,
                startSearchActive = true,
                onCloseSearch = {
                    showSearch = false
                    // Navigate to Home tab on search close
                    navController.navigate(Screen.Home.route) {
                        popUpTo(navController.graph.findStartDestination().id) { saveState = true }
                        launchSingleTop = true
                        restoreState = true
                    }
                },
                onSearchResultClick = { result ->
                    SearchResultNavigation.navigate(result, navController)
                    showSearch = false
                }
            )
        }
    }
}

@Composable
private fun TrueNasMobileNavGraph(
    navController: NavHostController,
    manager: TrueNASApiManager,
    rootNavController: NavController,
    onSearchClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    NavHost(
        navController = navController,
        startDestination = Screen.Home.route,
        modifier = modifier.background(MaterialTheme.colorScheme.background),
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
        composable(Screen.Home.route) {
            HomeScreen(
                manager,
                onNavigateToSettings = { rootNavController.navigate(Screen.Settings.route) },
                onPoolClick = { pool: System.Pool ->
                    PoolDataHolder.currentPool = pool
                    navController.navigate(Screen.PoolDetails.route)
                },
                onNavigateToShareInfo = { shareType ->
                    AppDataHolder.selectedShareType = shareType
                    navController.navigate(Screen.ShareInfo.route)
                },
                onDisksClick = {
                    navController.navigate(Screen.DiskInfo.route)
                },
                onNavigateToPerformance = {metricType ->
                    AppDataHolder.initialMetricType = metricType
                    navController.navigate(Screen.Performance.route)
                },
                onUpdateClick = {
                    navController.navigate(Screen.SystemUpdateScreen.route)
                },
                onInstanceConfigClick = {
                    navController.navigate(Screen.InstanceConfigScreen.route)
                },
                onSystemInfoClick = {
                    navController.navigate(Screen.SystemInformationScreen.route)
                },
                onSearchClick = onSearchClick
            )
        }
        composable(Screen.Storage.route) {
            StorageScreen(
                manager = manager,
                onNavigateToSettings = { rootNavController.navigate(Screen.Settings.route) },
                onPoolClick = { pool: System.Pool ->
                    PoolDataHolder.currentPool = pool
                    navController.navigate(Screen.PoolDetails.route)
                },
                onNavigateToShareInfo = { shareType ->
                    AppDataHolder.selectedShareType = shareType
                    navController.navigate(Screen.ShareInfo.route)
                },
                onDisksClick = {
                    navController.navigate(Screen.DiskInfo.route)
                },
                onSearchClick = onSearchClick
            )
        }
        composable(Screen.Tasks.route) {
            TasksScreen(
                manager = manager,
                onSearchClick = onSearchClick,
                onNavigateToAppInfo = { app ->
                    AppDataHolder.selectedApp = app
                    navController.navigate(Screen.AppDetailsScreen.route)
                },
                onOpenAdvanced = { appId ->
                    navController.navigate(Screen.AppAdvancedInfoScreen.createRoute(appId))
                },
                onNavigateToUpgrade = { appName ->
                    navController.navigate(Screen.AppUpgrade.createRoute(appName))
                },
                onNavigateToRollback = { navController.navigate(Screen.RollbackVersion.createRoute(it)) },
                onNavigateToMarketplace = { navController.navigate(Screen.Marketplace.route) },
                onNavigateToContainerInfo = { container ->
                    ContainerDataHolder.selectedContainer = container
                    navController.navigate(Screen.ContainerInfo.route)
                },
                onNavigateToVmInfo = { vmInfo ->
                    VmDataHolder.selectedVm = vmInfo
                    navController.navigate(Screen.VmDetails.route)
                }
            )
        }
        composable(Screen.LocalAdminSetupScreen.route) {
            LocalAdminSetupScreen(
                manager = manager,
                onNavigateBack = { navController.popBackStack() }
            )
        }
        composable(Screen.InstanceConfigScreen.route) {
            InstanceConfigScreen(
                manager = manager,
                onNavigateBack = { navController.popBackStack() },
                onNavigateToGeneralSystemSettings = {
                    navController.navigate(Screen.GeneralSystemSettingsScreen.route)
                },
                onNavigateToAdvancedSettings = {
                    navController.navigate(Screen.AdvancedSystemSettingsScreen.route)
                },
                onNavigateToServices = {
                    navController.navigate(Screen.ServicesScreen.route)
                },
                onNavigateToAlertSettings = {
                    navController.navigate(Screen.AlertServicesList.route)
                },
                onNavigateToUsers = {
                    navController.navigate(Screen.UserListScreen.route)
                },
                onNavigateToApiKeys = {
                    navController.navigate(Screen.ApiKeyListScreen.route)
                },
                onNavigateToAuditConfig = {
                    navController.navigate(Screen.AuditConfigScreen.route)
                },
                onNavigateToAuditLogs = {
                    navController.navigate(Screen.AuditLogsScreen.route)
                },
                onNavigateToNetwork = {
                    navController.navigate(Screen.NetworkScreen.route)
                },
                onNavigateToBoot = {
                    navController.navigate(Screen.BootScreen.route)
                },
                onNavigateToSystemInformation = {
                    navController.navigate(Screen.SystemInformationScreen.route)
                },
                onNavigateToTrueNasConnect = {
                    navController.navigate(Screen.TrueNasConnectScreen.route)
                },
                onNavigateToTrueCommand = {
                    navController.navigate(Screen.TrueCommandScreen.route)
                },
                onNavigateToAppImageManagement = {
                    navController.navigate(Screen.AppImageManagementScreen.route)
                }
            )
        }
        composable(Screen.AppImageManagementScreen.route) {
            AppImageManagementScreen(
                manager = manager,
                onNavigateToIxVolumes = {
                    navController.navigate(Screen.IxVolumeListScreen.route)
                },
                onNavigateToDockerImages = {
                    navController.navigate(Screen.DockerImageListScreen.route)
                },
                onNavigateBack = { navController.popBackStack() }
            )
        }
        composable(Screen.IxVolumeListScreen.route) {
            IxVolumeListScreen(
                manager = manager,
                onNavigateBack = { navController.popBackStack() }
            )
        }
        composable(Screen.DockerImageListScreen.route) {
            DockerImageListScreen(
                manager = manager,
                onNavigateBack = { navController.popBackStack() }
            )
        }
        composable(Screen.SystemInformationScreen.route) {
            SystemInformationScreen(
                manager = manager,
                onNavigateBack = { navController.popBackStack() },
                onNavigateToSoftwareInformation = {
                    navController.navigate(Screen.SoftwareInformationScreen.route)
                },
                onNavigateToHardwareInformation = {
                    navController.navigate(Screen.HardwareInformationScreen.route)
                }
            )
        }
        composable(Screen.SoftwareInformationScreen.route) {
            SoftwareInformationScreen(
                manager = manager,
                onNavigateBack = { navController.popBackStack() }
            )
        }
        composable(Screen.HardwareInformationScreen.route) {
            HardwareInformationScreen(
                manager = manager,
                onNavigateBack = { navController.popBackStack() },
                onNavigateToDisks = {
                    navController.navigate(Screen.DiskInfo.route)
                }
            )
        }
        composable(Screen.TrueNasConnectScreen.route) {
            TrueNasConnectScreen(
                manager = manager,
                onNavigateBack = { navController.popBackStack() }
            )
        }
        composable(Screen.TrueCommandScreen.route) {
            TrueCommandScreen(
                manager = manager,
                onNavigateBack = { navController.popBackStack() }
            )
        }
        composable(Screen.BootScreen.route) {
            BootScreen(
                manager = manager,
                onNavigateBack = { navController.popBackStack() },
                onNavigateToBootPool = {
                    navController.navigate(Screen.BootPoolScreen.route)
                },
                onNavigateToBootEnvironments = {
                    navController.navigate(Screen.BootEnvironmentsScreen.route)
                }
            )
        }
        composable(Screen.BootPoolScreen.route) {
            BootPoolScreen(
                manager = manager,
                onNavigateBack = { navController.popBackStack() }
            )
        }
        composable(Screen.BootEnvironmentsScreen.route) {
            BootEnvironmentsScreen(
                manager = manager,
                onNavigateBack = { navController.popBackStack() },
                onNavigateToDetail = { envId ->
                    navController.navigate(Screen.BootEnvironmentDetailScreen.createRoute(envId))
                }
            )
        }
        composable(
            Screen.BootEnvironmentDetailScreen.route,
            arguments = listOf(navArgument("environmentId") { type = NavType.StringType })
        ) { backStackEntry ->
            val envId = backStackEntry.arguments?.getString("environmentId").orEmpty()
            BootEnvironmentDetailScreen(
                manager = manager,
                environmentId = envId,
                onNavigateBack = { navController.popBackStack() }
            )
        }
        composable(Screen.NetworkScreen.route) {
            NetworkScreen(
                manager = manager,
                onNavigateBack = { navController.popBackStack() },
                onNavigateToEdit = {
                    navController.navigate(Screen.NetworkEditScreen.route)
                }
            )
        }
        composable(Screen.NetworkEditScreen.route) {
            NetworkEditScreen(
                manager = manager,
                onNavigateBack = { navController.popBackStack() }
            )
        }
        composable(Screen.AuditConfigScreen.route) {
            val vm: AuditConfigViewModel = viewModel(factory = AuditConfigViewModel.AuditConfigViewModelFactory(manager))
            AuditConfigScreen(
                manager = manager,
                viewModel = vm,
                onNavigateBack = { navController.popBackStack() }
            )
        }
        composable(Screen.AuditLogsScreen.route) {
            val vm: AuditLogsViewModel = viewModel(factory = AuditLogsViewModel.AuditLogsViewModelFactory(manager))
            AuditLogsScreen(
                manager = manager,
                viewModel = vm,
                onNavigateBack = { navController.popBackStack() }
            )
        }
        composable(Screen.UserListScreen.route) {
            UserListScreen(
                manager = manager,
                onNavigateBack = { navController.popBackStack() },
                onNavigateToUserDetail = { userId ->
                    navController.navigate(Screen.UserDetailScreen.createRoute(userId))
                },
                onNavigateToCreateUser = {
                    navController.navigate(Screen.UserCreateScreen.route)
                }
            )
        }
        composable(Screen.ApiKeyListScreen.route) {
            ApiKeyListScreen(
                manager = manager,
                onNavigateBack = { navController.popBackStack() },
                onNavigateToDetail = { keyId ->
                    navController.navigate(Screen.ApiKeyDetailScreen.createRoute(keyId))
                },
                onNavigateToCreate = {
                    navController.navigate(Screen.ApiKeyCreateScreen.route)
                }
            )
        }

        composable(
            route = Screen.ApiKeyDetailScreen.route,
            arguments = listOf(navArgument("keyId") { type = NavType.IntType })
        ) { backStackEntry ->
            val keyId = backStackEntry.arguments?.getInt("keyId") ?: 0
            ApiKeyDetailScreen(
                keyId = keyId,
                manager = manager,
                onNavigateBack = { navController.popBackStack() }
            )
        }

        composable(Screen.ApiKeyCreateScreen.route) {
            ApiKeyCreateScreen(
                manager = manager,
                onNavigateBack = { navController.popBackStack() }
            )
        }

        composable(Screen.GeneralSystemSettingsScreen.route) {
            GeneralSystemSettingsScreen(
                manager = manager,
                onNavigateBack = { navController.popBackStack() },
                onNavigateToEdit = {
                    navController.navigate(Screen.GeneralSystemSettingsEditScreen.route)
                }
            )
        }

        composable(Screen.GeneralSystemSettingsEditScreen.route) {
            GeneralSystemSettingsEditScreen(
                manager = manager,
                onNavigateBack = { navController.popBackStack() },
                onCheckinNavigateBack = {
                    navController.popBackStack()
                }
            )
        }

        composable(Screen.AdvancedSystemSettingsScreen.route) {
            AdvancedSystemSettingsScreen(
                manager = manager,
                onNavigateBack = { navController.popBackStack() },
                onNavigateToEdit = {
                    navController.navigate(Screen.AdvancedSystemSettingsEditScreen.route)
                }
            )
        }

        composable(Screen.AdvancedSystemSettingsEditScreen.route) {
            AdvancedSystemSettingsEditScreen(
                manager = manager,
                onNavigateBack = { navController.popBackStack() }
            )
        }

        composable(
            route = Screen.UserDetailScreen.route,
            arguments = listOf(navArgument("userId") { type = NavType.IntType })
        ) { backStackEntry ->
            val userId = backStackEntry.arguments?.getInt("userId") ?: 0
            UserDetailScreen(
                userId = userId,
                manager = manager,
                onNavigateBack = { navController.popBackStack() }
            )
        }

        composable(Screen.UserCreateScreen.route) {
            UserCreateScreen(
                manager = manager,
                onNavigateBack = { navController.popBackStack() }
            )
        }
        composable(Screen.ServicesScreen.route) {
            ServicesScreen(
                manager = manager,
                onNavigateBack = { navController.popBackStack() },
                onNavigateToServiceDetail = { service ->
                    AppDataHolder.selectedService = service
                    navController.navigate(Screen.ServicesDetailScreen.route)
                }
            )
        }
        composable(Screen.ServicesDetailScreen.route) {
            val service = AppDataHolder.selectedService
            if (service != null) {
                ServiceDetailScreen(
                    service = service,
                    manager = manager,
                    onNavigateBack = { navController.popBackStack() }
                )
            }
        }
        composable(Screen.SystemUpdateScreen.route) {
            val updateVersions by AppCache.cachedUpdateVersions.collectAsState()
            val systemInfo by AppCache.cachedSystemInfo.collectAsState()

            SystemUpdateScreen(
                manager = manager,
                versions = updateVersions,
                currentVersion = systemInfo?.version,
                onNavigateBack = { navController.popBackStack() }
            )
        }
        composable(Screen.AlertServicesList.route) {
            AlertServicesListScreen(
                manager = manager,
                onNavigateBack = { navController.popBackStack() },
                onNavigateToDetail = { service ->
                    navController.navigate(Screen.AlertServiceDetail.createRoute(service.id))
                },
                onNavigateToCreate = {
                    navController.navigate(Screen.AlertServiceCreate.route)
                },
                onNavigateToClassesConfig = {
                    navController.navigate(Screen.AlertClassesConfig.route)
                }
            )
        }
        composable(
            route = Screen.AlertServiceDetail.route,
            arguments = listOf(navArgument("serviceId") { type = NavType.IntType })
        ) { backStackEntry ->
            val serviceId = backStackEntry.arguments?.getInt("serviceId") ?: 0
            AlertServiceDetailScreen(
                serviceId = serviceId,
                manager = manager,
                onNavigateBack = { navController.popBackStack() }
            )
        }
        composable(Screen.AlertServiceCreate.route) {
            AlertServiceCreateScreen(
                manager = manager,
                onNavigateBack = { navController.popBackStack() }
            )
        }
        composable(Screen.AlertClassesConfig.route) {
            AlertClassesConfigScreen(
                manager = manager,
                onNavigateBack = { navController.popBackStack() }
            )
        }
        composable(Screen.DiskInfo.route){
            val disks = AppDataHolder.disks
            DiskInfoScreen(
                disks,
                manager
            ) {
                navController.popBackStack()
            }
        }
        composable(Screen.AppConfigScreen.route){
            val currAppValues = AppDataHolder.selectedAppValues
            AppConfigScreen(
                manager,
                currAppValues,
                {
                    navController.popBackStack()
                }
            )
        }

        composable(Screen.ShareInfo.route) {
            val shareType = AppDataHolder.selectedShareType
            if (shareType != null) {
                ShareInfoScreen(
                    shareType = shareType,
                    manager = manager,
                    onNavigateBack = { navController.popBackStack() }
                )
            }
        }
        composable(Screen.Performance.route) {
            val performanceViewModel: HomeViewModel = viewModel(
                factory = HomeViewModel.HomeViewModelFactory(
                    manager,
                    LocalContext.current.applicationContext
                )
            )
            val performanceState by performanceViewModel.uiState.collectAsState()
            val performanceSuccess = performanceState as? HomeUiState.Success
            LaunchedEffect(performanceSuccess) {
                if (performanceSuccess != null) {
                    AppDataHolder.cpuData = performanceSuccess.cpuData
                    AppDataHolder.memoryData = performanceSuccess.memoryData
                    AppDataHolder.temperatureData = performanceSuccess.temperatureData
                }
            }
            PerformanceScreen(
                cpuData = performanceSuccess?.cpuData ?: AppDataHolder.cpuData,
                memoryData = performanceSuccess?.memoryData ?: AppDataHolder.memoryData,
                temperatureData = performanceSuccess?.temperatureData ?: AppDataHolder.temperatureData,
                initialMetricType = AppDataHolder.initialMetricType,
                isLoading = performanceState is HomeUiState.Loading,
                manager = manager,
                onNavigateBack = null,
                onRefresh = { performanceViewModel.refresh() }
            )
        }

        composable(Screen.Apps.route) {
            AppsScreen(
                manager = manager,
                onNavigateToAppInfo = { app ->
                    AppDataHolder.selectedApp = app
                    navController.navigate(Screen.AppDetailsScreen.route)
                },
                onOpenAdvanced = { appId ->
                    navController.navigate(Screen.AppAdvancedInfoScreen.createRoute(appId))
                },
                onNavigateToUpgrade = { appName ->
                    navController.navigate(Screen.AppUpgrade.createRoute(appName)) },
                onNavigateToRollback = { navController.navigate(Screen.RollbackVersion.createRoute(it)) },
                onNavigateToMarketplace = { navController.navigate(Screen.Marketplace.route) },
                onSearchClick = onSearchClick
            )
        }

        composable(Screen.PoolDetails.route) {
            PoolDetailsScreen(
                manager = manager,
                onNavigateBack = { navController.popBackStack() },
                onNavigateToFiles = { poolName: String -> navController.navigate(Screen.DatasetExplorer.createRoute(poolName)) }
            )
        }
        composable(
            route = Screen.RollbackVersion.route,
            arguments = listOf(navArgument("appName") { type = NavType.StringType })
        ) { backStackEntry ->
            val appName = backStackEntry.arguments?.getString("appName") ?: ""
            val context = LocalContext.current
            val appsViewModel: AppsScreenViewModel = viewModel(factory = AppsScreenViewModel.AppsScreenViewModelFactory(manager))
            val uiState by appsViewModel.uiState.collectAsState()

            LaunchedEffect(appName) {
                appsViewModel.loadRollbackVersions(appName)
            }

            RollbackVersionScreen(
                appName = appName,
                versions = uiState.rollbackVersions,
                isLoadingVersions = uiState.isLoadingRollbackVersions,
                fetchError = uiState.error,
                onConfirmRollback = { targetVersion, rollbackSnapshot ->
                    appsViewModel.rollbackApp(context, appName, targetVersion, rollbackSnapshot)
                },
                onNavigateBack = { navController.popBackStack() }
            )
        }

        composable(Screen.AppDetailsScreen.route) {
            val app = AppDataHolder.selectedApp
            if (app == null) {
                MissingSelectionScreen(onNavigateBack = { navController.popBackStack() })
            } else {
            val appsViewModel: AppsScreenViewModel = viewModel(factory = AppsScreenViewModel.AppsScreenViewModelFactory(manager))
            LaunchedEffect(Unit) {
                if (appsViewModel.uiState.value.marketplaceApps.isEmpty()) appsViewModel.loadMarketplaceApps()
            }
            AppInfoScreen(
                app = app,
                manager = manager,
                onNavigateBack = { navController.popBackStack() },
                onNavigateToMarketplaceCategory = { categoryName -> navController.navigate(Screen.MarketplaceCategory.createRoute(categoryName)) },
                onNavigateToMarketplaceAppDetails = { appName ->
                    val matchedAvailableItem = appsViewModel.uiState.value.marketplaceApps.find { it.name == appName }
                    if (matchedAvailableItem != null) {
                        AppDataHolder.selectedMarketplaceApp = matchedAvailableItem
                        navController.navigate("marketplace_app_details")
                    } else {
                        navController.navigate("marketplace?category=")
                    }
                },
                onDeleteSuccess = { navController.navigate(Screen.Apps.route) },
                onEditClick = {appName , appTrain->
                    AppDataHolder.selectedAppValues = AppConfigPageValues(
                        appName,
                        appTrain
                    )
                    navController.navigate(Screen.AppConfigScreen.route)
                },
                onOpenAdvanced = { appId ->
                    navController.navigate(Screen.AppAdvancedInfoScreen.createRoute(appId))
                }
            )
            }
        }

        composable(
            route = Screen.AppAdvancedInfoScreen.route,
            arguments = listOf(navArgument("appId") { type = NavType.StringType })
        ) { backStackEntry ->
            val appId = backStackEntry.arguments?.getString("appId").orEmpty()
            AppAdvancedInfoScreen(
                manager = manager,
                appId = appId,
                onNavigateBack = { navController.popBackStack() }
            )
        }

        composable(
            route = Screen.MarketplaceCategory.route,
            arguments = listOf(navArgument("category") { type = NavType.StringType; defaultValue = ""; nullable = true })
        ) { backStackEntry ->
            val category = backStackEntry.arguments?.getString("category")?.takeIf { it.isNotBlank() }
            val appsViewModel: AppsScreenViewModel = viewModel(factory = AppsScreenViewModel.AppsScreenViewModelFactory(manager))
            MarketplaceScreen(manager = manager, initialCategory = category, onNavigateBack = { navController.popBackStack() }, onMarketplaceApplicationClicked = { app -> AppDataHolder.selectedMarketplaceApp = app; navController.navigate("marketplace_app_details") }, onInstallApplication = { app -> appsViewModel.loadCatalogAppDetails(app.name, app.train); navController.navigate(Screen.CatalogInstall.createRoute(app.name, app.train)) })
        }

        composable(Screen.Marketplace.route) {
            val appsViewModel: AppsScreenViewModel = viewModel(factory = AppsScreenViewModel.AppsScreenViewModelFactory(manager))
            MarketplaceScreen(manager = manager, onNavigateBack = { navController.popBackStack() }, onMarketplaceApplicationClicked = { app -> AppDataHolder.selectedMarketplaceApp = app; navController.navigate("marketplace_app_details") }, onInstallApplication = { app -> appsViewModel.loadCatalogAppDetails(app.name, app.train); navController.navigate(Screen.CatalogInstall.createRoute(app.name, app.train)) })
        }

        composable(
            route = Screen.AppUpgrade.route,
            arguments = listOf(navArgument("appName")
            { type = NavType.StringType })
        ) { backStackEntry ->
            val appName = backStackEntry.arguments?.getString("appName") ?: ""
            val context = LocalContext.current
            val viewModel: AppsScreenViewModel = viewModel(factory = AppsScreenViewModel.AppsScreenViewModelFactory(manager))
            val uiState by viewModel.uiState.collectAsState()
            LaunchedEffect(appName) { viewModel.clearUpgradeSummary(); viewModel.loadUpgradeSummary(appName) }
            val currentApp = uiState.apps.find { it.name == appName }
            val currentVersion = currentApp?.version ?: "Unknown"
            val currentHumanVersion = currentApp?.metadata?.appVersion
            val summary = uiState.upgradeSummaryResult
            val isLoading = (uiState.isLoadingUpgradeSummaryForApp == appName) && (summary == null)
            if (isLoading) {
                LoadingScreen("Checking upgrades...")
            } else if (summary != null) {
                UpgradeSummaryScreen(
                    appName = appName,
                    summary = summary,
                    currentVersion = currentVersion,
                    currentHumanVersion = currentHumanVersion,
                    manager = manager,
                    canUpgrade = currentApp?.canUpgradeNow() ?: false,
                    appState = currentApp?.state ?: "",
                    onConfirmUpgrade = { version, backup ->
                        viewModel.upgradeApp(appName, context, version, backup)
                    },
                    onNavigateBack = { navController.popBackStack() }
                )
            } else if (uiState.error != null) {
                LaunchedEffect(Unit) { navController.popBackStack() }
            }
        }

        composable(Screen.MarketplaceAppDetails.route) {
            val appsViewModel: AppsScreenViewModel = viewModel(factory = AppsScreenViewModel.AppsScreenViewModelFactory(manager))
            val app = AppDataHolder.selectedMarketplaceApp
            if (app != null) {
                MarketplaceAppDetailsScreen(app = app, onNavigateBack = { navController.popBackStack() }, manager = manager, onInstallClick = { appName, train -> appsViewModel.loadCatalogAppDetails(appName, train); navController.navigate(Screen.CatalogInstall.createRoute(appName,train)) })
            }
        }

        composable(route = Screen.CatalogInstall.route, arguments = listOf(navArgument("appName") { type = NavType.StringType }, navArgument("train") { type = NavType.StringType })) { backStackEntry ->
            val appsViewModel: AppsScreenViewModel = viewModel(factory = AppsScreenViewModel.AppsScreenViewModelFactory(manager))
            val appName = backStackEntry.arguments?.getString("appName") ?: ""
            val train = backStackEntry.arguments?.getString("train") ?: ""
            MarketplaceAppInstallScreen(appName = appName, train = train, viewModel = appsViewModel, manager = manager, onBack = { navController.popBackStack() }, onInstallSuccess = { navController.popBackStack() })
        }

        composable(Screen.Containers.route) {
            ContainersScreen(
                manager = manager,
                onNavigateToContainerInfo = { container -> ContainerDataHolder.selectedContainer = container; navController.navigate("container_info") },
                onSearchClick = onSearchClick
            )
        }

        composable(Screen.ContainerInfo.route) {
            val container = ContainerDataHolder.selectedContainer
            if (container == null) {
                MissingSelectionScreen(onNavigateBack = { navController.popBackStack() })
            } else {
                ContainerInfoScreen(manager = manager, container = container, onNavigateBack = { navController.popBackStack() })
            }
        }

        composable(Screen.Vms.route) {
            VmsScreen(
                manager = manager,
                onNavigateToVmInfo = { vmInfo -> VmDataHolder.selectedVm = vmInfo; navController.navigate("vm_details") },
                onSearchClick = onSearchClick
            )
        }

        composable(Screen.VmDetails.route) {
            val vm = VmDataHolder.selectedVm
            if (vm == null) {
                MissingSelectionScreen(onNavigateBack = { navController.popBackStack() })
            } else {
                VmInfoScreen(vm = vm, manager = manager, onNavigateBack = { navController.popBackStack() })
            }
        }

        composable(route = Screen.DatasetExplorer.route, arguments = listOf(navArgument("poolName") { type = NavType.StringType })) { backStackEntry ->
            val poolName = backStackEntry.arguments?.getString("poolName") ?: ""
            DatasetExplorerScreen(manager = manager, onNavigateBack = { navController.popBackStack() }, poolName = poolName)
        }
    }
}

private fun onNavClick(navController: NavController, route: String) {
    navController.navigate(route) {
        popUpTo(navController.graph.findStartDestination().id) { saveState = true }
        launchSingleTop = true
        restoreState = true
    }
}

/**
 * Shown when a detail screen is opened without its backing data.
 *
 * Detail screens are fed from in-memory holders (AppDataHolder,
 * ContainerDataHolder, VmDataHolder) that are seeded right before navigation.
 * After process death (or via a restored back stack) the holder is empty, and
 * dereferencing it would crash. We tell the user what happened and send them
 * back instead of showing a blank screen or throwing.
 */
@Composable
private fun MissingSelectionScreen(onNavigateBack: () -> Unit) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .padding(32.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Icon(
            imageVector = Icons.Default.Info,
            contentDescription = null,
            modifier = Modifier.size(48.dp),
            tint = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Spacer(modifier = Modifier.height(16.dp))
        Text(
            text = stringResource(R.string.selection_expired_title),
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.SemiBold,
            color = MaterialTheme.colorScheme.onSurface,
            textAlign = TextAlign.Center
        )
        Spacer(modifier = Modifier.height(8.dp))
        Text(
            text = stringResource(R.string.selection_expired_hint),
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center
        )
        Spacer(modifier = Modifier.height(24.dp))
        OutlinedButton(onClick = onNavigateBack) {
            Text(stringResource(R.string.common_back))
        }
    }
}