package com.gegaremant.truenasmobile.ui.utils

import com.gegaremant.truenasmobile.ui.homepage.pools.PoolDataHolder
import com.gegaremant.truenasmobile.ui.services.apps.details.appdetails.AppDataHolder
import com.gegaremant.truenasmobile.ui.services.containers.details.ContainerDataHolder
import com.gegaremant.truenasmobile.ui.services.vm.details.VmDataHolder

/**
 * The "selected entity" holders that detail screens are navigated with.
 *
 * They are separate process-wide objects rather than arguments, so a pool, app,
 * container or VM picked on one TrueNAS stays in memory after the user switches
 * to another one. Every navigation writes the holder before opening the screen,
 * so in practice the stale value is unused - but it is the previous server's
 * data sitting in a global, and a screen reached from a restored back stack has
 * no way to tell whose data it is looking at.
 *
 * Cleared wherever the session changes: sign-out, a new login, a profile switch.
 */
object NavigationHolders {
    fun clearAll() {
        AppDataHolder.clear()
        PoolDataHolder.clear()
        ContainerDataHolder.clear()
        VmDataHolder.clear()
    }
}
