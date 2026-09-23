package com.imnotndesh.truehub.data.api

import com.imnotndesh.truehub.data.ApiResult
import com.imnotndesh.truehub.data.models.Alerts
import com.squareup.moshi.Types

class AlertsService(val manager: TrueNASApiManager) {
    suspend fun getAlertServiceWithResult(id: Int): ApiResult<Alerts.AlertServiceEntry> {
        return manager.callWithResult(
            method = ApiMethods.Alerts.ALERT_SERVICE_GET_INSTANCE,
            params = listOf(id),
            resultType = Alerts.AlertServiceEntry::class.java
        )
    }

    suspend fun deleteAlertServiceWithResult(id: Int): ApiResult<Boolean> {
        return manager.callWithResult(
            method = ApiMethods.Alerts.ALERT_SERVICE_DELETE,
            params = listOf(id),
            resultType = Boolean::class.java
        )
    }

    suspend fun createAlertServiceWithResult(service: Alerts.AlertServiceCreate): ApiResult<Alerts.AlertServiceEntry> {
        return manager.callWithResult(
            method = ApiMethods.Alerts.ALERT_SERVICE_CREATE,
            params = listOf(service),
            resultType = Alerts.AlertServiceEntry::class.java
        )
    }

    suspend fun listAlertServicesWithResult(): ApiResult<List<Alerts.AlertServiceEntry>> {
        val type = Types.newParameterizedType(List::class.java, Alerts.AlertServiceEntry::class.java)
        return manager.callWithResult(
            method = ApiMethods.Alerts.ALERT_SERVICE_QUERY,
            params = listOf(),
            resultType = type
        )
    }

    suspend fun testAlertServiceWithResult(service: Alerts.AlertServiceCreate): ApiResult<Boolean> {
        return manager.callWithResult(
            method = ApiMethods.Alerts.ALERT_SERVICE_TEST,
            params = listOf(service),
            resultType = Boolean::class.java
        )
    }

    suspend fun updateAlertServiceWithResult(
        id: Int,
        service: Alerts.AlertServiceUpdate
    ): ApiResult<Alerts.AlertServiceEntry> {
        return manager.callWithResult(
            method = ApiMethods.Alerts.ALERT_SERVICE_UPDATE,
            params = listOf(id, service),
            resultType = Alerts.AlertServiceEntry::class.java
        )
    }

    suspend fun updateAlertClassesWithResult(
        update: Alerts.AlertClassUpdate
    ): ApiResult<Alerts.AlertClassesEntry> {
        return manager.callWithResult(
            method = ApiMethods.Alerts.ALERTCLASSES_UPDATE,
            params = listOf(update),
            resultType = Alerts.AlertClassesEntry::class.java
        )
    }
    suspend fun listAlertCategoriesWithResult(): ApiResult<List<com.imnotndesh.truehub.data.models.System.AlertCategoriesResponse>> {
        return manager.system.listCategoriesWithResult()
    }

    suspend fun listAlertPoliciesWithResult(): ApiResult<List<String>> {
        return manager.system.listAlertPoliciesWithResult()
    }
    suspend fun getAlertClassesConfigWithResult(): ApiResult<Alerts.AlertClassesEntry> {
        return manager.callWithResult(
            method = ApiMethods.Alerts.ALERTCLASSES_CONFIG,
            params = emptyList(),
            resultType = Alerts.AlertClassesEntry::class.java
        )
    }
}