package com.gegaremant.truenasmobile.ui.utils

import com.gegaremant.truenasmobile.data.models.Apps
import com.gegaremant.truenasmobile.data.models.Shares
import com.gegaremant.truenasmobile.data.models.System
import com.gegaremant.truenasmobile.data.models.Virt
import com.gegaremant.truenasmobile.data.models.Vm
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

object AppCache {
    private val _cachedApps = MutableStateFlow<List<Apps.AppQueryResponse>>(emptyList())
    val cachedApps: StateFlow<List<Apps.AppQueryResponse>> = _cachedApps.asStateFlow()
    private val _cachedSystemUpdateVersions = MutableStateFlow<List<System.UpdateAvailableVersionsResponse>>(emptyList())
    val cachedUpdateVersions : StateFlow<List<System.UpdateAvailableVersionsResponse>> = _cachedSystemUpdateVersions.asStateFlow()

    private val _cachedSystemInfo = MutableStateFlow<System.SystemInfo?>(null)
    val cachedSystemInfo: StateFlow<System.SystemInfo?> = _cachedSystemInfo.asStateFlow()

    private val _cachedMarketplaceApps = MutableStateFlow<List<Apps.AppAvailableItem>>(emptyList())
    val cachedMarketplaceApps: StateFlow<List<Apps.AppAvailableItem>> = _cachedMarketplaceApps.asStateFlow()

    private val _cachedPools = MutableStateFlow<List<System.Pool>>(emptyList())
    val cachedPools: StateFlow<List<System.Pool>> = _cachedPools.asStateFlow()

    private val _cachedDisks = MutableStateFlow<List<System.DiskDetails>>(emptyList())
    val cachedDisks: StateFlow<List<System.DiskDetails>> = _cachedDisks.asStateFlow()

    private val _cachedSmbShares = MutableStateFlow<List<Shares.SmbShare>>(emptyList())
    val cachedSmbShares: StateFlow<List<Shares.SmbShare>> = _cachedSmbShares.asStateFlow()

    private val _cachedNfsShares = MutableStateFlow<List<Shares.NfsShare>>(emptyList())
    val cachedNfsShares: StateFlow<List<Shares.NfsShare>> = _cachedNfsShares.asStateFlow()

    private val _cachedContainers = MutableStateFlow<List<Virt.ContainerResponse>>(emptyList())
    val cachedContainers: StateFlow<List<Virt.ContainerResponse>> = _cachedContainers.asStateFlow()
    private val _cachedVms = MutableStateFlow<List<Vm.VmQueryResponse>>(emptyList())
    val cachedVms: StateFlow<List<Vm.VmQueryResponse>> = _cachedVms.asStateFlow()

    private val _cachedServices = MutableStateFlow<List<System.ServiceQueryResponse>>(emptyList())
    val cachedServices: StateFlow<List<System.ServiceQueryResponse>> = _cachedServices.asStateFlow()

    fun updateApps(apps: List<Apps.AppQueryResponse>) {
        _cachedApps.value = apps
    }
    fun updateSystemUpdateVersions(versions : List<System.UpdateAvailableVersionsResponse>){
        _cachedSystemUpdateVersions.value = versions
    }

    fun updateSystemInfo(info: System.SystemInfo) {
        _cachedSystemInfo.value = info
    }

    fun updateMarketplaceApps(apps : List<Apps.AppAvailableItem>){
        _cachedMarketplaceApps.value = apps
    }

    fun updatePools(pools: List<System.Pool>) {
        _cachedPools.value = pools
    }

    fun updateDisks(disks: List<System.DiskDetails>) {
        _cachedDisks.value = disks
    }

    fun updateSmbShares(shares: List<Shares.SmbShare>) {
        _cachedSmbShares.value = shares
    }

    fun updateNfsShares(shares: List<Shares.NfsShare>) {
        _cachedNfsShares.value = shares
    }

    fun updateContainers(containers: List<Virt.ContainerResponse>) {
        _cachedContainers.value = containers
    }

    fun updateVms(vms: List<Vm.VmQueryResponse>) {
        _cachedVms.value = vms
    }

    fun updateServices(services: List<System.ServiceQueryResponse>) {
        _cachedServices.value = services
    }

    /**
     * Drops everything.
     *
     * The cache is process-wide, so it outlives the session it was filled for:
     * without this, signing out of one TrueNAS and into another leaves the first
     * server's pools, apps, shares, services and system info in memory for the
     * search overlay and the dashboard to render. Called on sign-out and on
     * every successful login.
     */
    fun clearAllCache() {
        _cachedApps.value = emptyList()
        _cachedSystemUpdateVersions.value = emptyList()
        _cachedSystemInfo.value = null
        _cachedMarketplaceApps.value = emptyList()
        _cachedPools.value = emptyList()
        _cachedDisks.value = emptyList()
        _cachedSmbShares.value = emptyList()
        _cachedNfsShares.value = emptyList()
        _cachedContainers.value = emptyList()
        _cachedVms.value = emptyList()
        _cachedServices.value = emptyList()
    }
}