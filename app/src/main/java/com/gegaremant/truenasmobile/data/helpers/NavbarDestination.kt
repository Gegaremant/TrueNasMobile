package com.gegaremant.truenasmobile.data.helpers

import androidx.annotation.StringRes
import com.gegaremant.truenasmobile.R
import com.gegaremant.truenasmobile.ui.Screen

/**
 * Catalog of destinations that can appear on the bottom navigation bar.
 *
 * Home is required: it cannot be hidden and always sits first. Every other
 * destination is optional, and the user picks which ones are visible and in
 * which order — see `PersonalizationManager.saveNavbar` and the "Bottom
 * navigation" card in the theme screen.
 *
 * Labels live in the locale dictionaries via [titleRes]; nothing here is
 * hardcoded. Icons deliberately do NOT live here: this is a data-layer enum, and
 * the icon set is a UI concern, so `MainScreen` maps a destination to its
 * `NavItem(icon, titleRes)`.
 *
 * To add a destination: add the enum entry, point [titleRes] at a string in
 * both locale dictionaries, and map it to icons in
 * `MainScreen.destinationToNavItem`.
 */
enum class NavbarDestination(
    @StringRes val titleRes: Int,
    val route: String,
    val isRequired: Boolean = false
) {
    HOME(R.string.nav_statistics, Screen.Home.route, isRequired = true),
    STORAGE(R.string.nav_storage, Screen.Storage.route),
    TASKS(R.string.nav_tasks, Screen.Tasks.route),
    APPS(R.string.nav_apps, Screen.Apps.route),
    CONTAINERS(R.string.nav_containers, Screen.Containers.route),
    VMS(R.string.nav_vms, Screen.Vms.route),
    MARKETPLACE(R.string.nav_marketplace, Screen.Marketplace.route),
    INSTANCE_SETTINGS(R.string.nav_instance_settings, Screen.InstanceConfigScreen.route),
    UPDATES(R.string.nav_updates, Screen.SystemUpdateScreen.route);

    companion object {
        /** Entries the user is not allowed to remove or move. */
        val required get() = entries.filter { it.isRequired }

        /** Entries the user can show, hide and reorder. */
        val optional get() = entries.filter { !it.isRequired }

        /**
         * The bar the owner asked for: Details, Storage, Tasks - in that order.
         *
         * Apps, Containers and VMs are sub-tabs of Tasks now, so they are not
         * separate bar entries; Performance is gone as a tab because its charts
         * live on the Details screen. The remaining entries stay available in
         * the bar editor for anyone who wants them.
         */
        val defaults: List<NavbarDestination>
            get() = listOf(HOME, STORAGE, TASKS)
    }
}
