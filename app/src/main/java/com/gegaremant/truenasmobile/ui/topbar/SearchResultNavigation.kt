package com.gegaremant.truenasmobile.ui.topbar

import androidx.navigation.NavController
import com.gegaremant.truenasmobile.ui.Screen
import com.gegaremant.truenasmobile.ui.homepage.details.ShareType
import com.gegaremant.truenasmobile.ui.homepage.pools.PoolDataHolder
import com.gegaremant.truenasmobile.ui.services.apps.details.appdetails.AppDataHolder
import com.gegaremant.truenasmobile.ui.services.containers.details.ContainerDataHolder
import com.gegaremant.truenasmobile.ui.services.vm.details.VmDataHolder

/**
 * Maps a [SearchResult] to navigation actions on a [NavController].
 *
 * This is the single place that knows how to translate every search result
 * into the correct screen route + pre-navigation data setup (via the DataHolder pattern).
 */
object SearchResultNavigation {

    /**
     * Navigate to the destination represented by this search result.
     *
     * @param result    The search result to navigate to.
     * @param navController The NavController for the MainScreen's internal NavHost.
     */
    fun navigate(result: SearchResult, navController: NavController) {
        when (result) {
            is SearchResult.NavigationResult -> {
                // Static screen/subsection — just navigate to the route
                navController.navigate(result.destinationRoute) {
                    launchSingleTop = true
                }
            }

            is SearchResult.PoolResult -> {
                PoolDataHolder.currentPool = result.pool
                navController.navigate(Screen.PoolDetails.route) {
                    launchSingleTop = true
                }
            }

            is SearchResult.DiskResult -> {
                AppDataHolder.disks = listOf(result.disk)
                navController.navigate(Screen.DiskInfo.route) {
                    launchSingleTop = true
                }
            }

            is SearchResult.ShareResult -> {
                AppDataHolder.selectedShareType = ShareType.Smb(result.share)
                navController.navigate(Screen.ShareInfo.route) {
                    launchSingleTop = true
                }
            }

            is SearchResult.ServiceResult -> {
                AppDataHolder.selectedService = result.service
                navController.navigate(Screen.ServicesDetailScreen.route) {
                    launchSingleTop = true
                }
            }

            is SearchResult.SystemInfoResult -> {
                navController.navigate(Screen.SystemInformationScreen.route) {
                    launchSingleTop = true
                }
            }

            is SearchResult.InstalledAppResult -> {
                AppDataHolder.selectedApp = result.app
                navController.navigate(Screen.AppDetailsScreen.route) {
                    launchSingleTop = true
                }
            }

            is SearchResult.MarketplaceAppResult -> {
                AppDataHolder.selectedMarketplaceApp = result.app
                navController.navigate(Screen.MarketplaceAppDetails.route) {
                    launchSingleTop = true
                }
            }

            is SearchResult.ContainerResult -> {
                ContainerDataHolder.selectedContainer = result.container
                navController.navigate(Screen.ContainerInfo.route) {
                    launchSingleTop = true
                }
            }

            is SearchResult.VmResult -> {
                VmDataHolder.selectedVm = result.vm
                navController.navigate(Screen.VmDetails.route) {
                    launchSingleTop = true
                }
            }

            is SearchResult.InstanceSettingsResult -> {
                navController.navigate(result.route) {
                    launchSingleTop = true
                }
            }

            is SearchResult.ActionResult -> {
                // Not reachable: system actions were removed from the search
                // index (see SearchViewModel). Kept as a no-op so that an
                // unexpected ActionResult can never crash navigation.
            }
        }
    }
}
