package com.imnotndesh.truehub.ui.utils

import com.imnotndesh.truehub.data.models.Apps
import com.imnotndesh.truehub.data.models.Shares
import com.imnotndesh.truehub.data.models.System
import com.imnotndesh.truehub.data.models.Virt
import com.imnotndesh.truehub.data.models.Vm
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

    fun clearAllCache() {
        _cachedApps.value = emptyList()
        _cachedSystemInfo.value = null
        _cachedPools.value = emptyList()
        _cachedDisks.value = emptyList()
        _cachedSmbShares.value = emptyList()
        _cachedNfsShares.value = emptyList()
        _cachedContainers.value = emptyList()
        _cachedVms.value = emptyList()
    }
}