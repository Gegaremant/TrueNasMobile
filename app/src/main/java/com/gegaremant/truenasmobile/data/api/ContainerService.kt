package com.gegaremant.truenasmobile.data.api

import com.gegaremant.truenasmobile.data.ApiResult
import com.gegaremant.truenasmobile.data.models.Container
import com.squareup.moshi.Types

/**
 * Containers, speaking the TrueNAS 26.0 `container.*` API.
 *
 * This replaces the old `virt.instance.*` calls. TrueNAS 26.0 removed the
 * `virt.*` namespace entirely - verified against a 26.0.0-BETA.3 stand, where
 * `virt.instance.query` answers `-32601 Method does not exist` and
 * `core.get_methods` reports zero `virt.*` methods.
 *
 * Two behavioural differences from the old service are deliberate:
 *  - there is no `container.restart`, so [restartContainerWithResult] is a
 *    stop followed by a start;
 *  - job ids come back as a plain number, so every mutating call is typed
 *    consistently as [Double] (the previous code declared one of them `Int`
 *    and then called `toInt()` on it).
 */
class ContainerService(val manager: TrueNASApiManager) {

    private val responseType = Container.ContainerResponse::class.java

    private val responseListType =
        Types.newParameterizedType(List::class.java, Container.ContainerResponse::class.java)

    // ── Reads ───────────────────────────────────────────────────────────────

    suspend fun getContainersWithResult(): ApiResult<List<Container.ContainerResponse>> =
        manager.callWithResult(
            method = ApiMethods.Container.QUERY,
            params = listOf(listOf<Map<String, String>>(), mapOf("extra" to mapOf("include_minimum" to false))),
            resultType = responseListType
        )

    suspend fun getContainerWithResult(id: String): ApiResult<Container.ContainerResponse> =
        manager.callWithResult(
            method = ApiMethods.Container.GET_INSTANCE,
            params = listOf(id),
            resultType = responseType
        )

    suspend fun getContainerDevicesWithResult(id: String): ApiResult<List<Container.Device>> {
        val type = Types.newParameterizedType(List::class.java, Container.Device::class.java)
        return manager.callWithResult(
            method = ApiMethods.Container.QUERY_DEVICES,
            params = listOf(id),
            resultType = type
        )
    }

    // ── Mutations ───────────────────────────────────────────────────────────

    suspend fun startContainerWithResult(id: String): ApiResult<Double> =
        manager.callWithResult(
            method = ApiMethods.Container.START,
            params = listOf(id),
            resultType = Double::class.java
        )

    suspend fun stopContainerWithResult(
        id: String,
        timeout: Int? = -1,
        force: Boolean? = false
    ): ApiResult<Double> =
        manager.callWithResult(
            method = ApiMethods.Container.STOP,
            params = listOf(id, Container.stopArgs(timeout, force)),
            resultType = Double::class.java
        )

    /**
     * Restart by stopping and starting again.
     *
     * `container.restart` does not exist in the 26.0 API, so this mirrors what
     * the middleware used to do. The stop is honoured first: if it fails we
     * report the failure instead of starting a container the user asked to be
     * restarted.
     */
    suspend fun restartContainerWithResult(
        id: String,
        timeout: Int? = -1,
        force: Boolean? = false
    ): ApiResult<Double> {
        val stopped = stopContainerWithResult(id, timeout, force)
        if (stopped is ApiResult.Error) return stopped
        return startContainerWithResult(id)
    }

    suspend fun deleteContainerWithResult(id: String): ApiResult<Double> =
        manager.callWithResult(
            method = ApiMethods.Container.DELETE,
            params = listOf(id),
            resultType = Double::class.java
        )

    suspend fun updateContainerWithResult(
        id: String,
        newInfo: Container.ContainerUpdate
    ): ApiResult<Container.ContainerResponse> =
        manager.callWithResult(
            method = ApiMethods.Container.UPDATE,
            params = listOf(id, newInfo),
            resultType = responseType
        )

    suspend fun deleteContainerDeviceWithResult(id: String, deviceName: String): ApiResult<Boolean> =
        manager.callWithResult(
            method = ApiMethods.Container.DELETE_DEVICE,
            params = listOf(id, deviceName),
            resultType = Boolean::class.java
        )

    suspend fun queryRegistryWithResult(): ApiResult<List<Container.ImageChoice>> {
        val type = Types.newParameterizedType(List::class.java, Container.ImageChoice::class.java)
        return manager.callWithResult(
            method = ApiMethods.Container.QUERY_REGISTRY,
            params = listOf(),
            resultType = type
        )
    }
}