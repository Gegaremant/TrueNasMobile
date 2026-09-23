package com.gegaremant.truenasmobile.ui.services.apps.details.appdetails

import com.gegaremant.truenasmobile.data.models.Apps
import com.gegaremant.truenasmobile.data.models.System
import com.gegaremant.truenasmobile.ui.homepage.details.MetricType
import com.gegaremant.truenasmobile.ui.homepage.details.ShareType

object AppDataHolder {
    var selectedApp: Apps.AppQueryResponse? = null
    var selectedMarketplaceApp: Apps.AppAvailableItem ?= null
    var disks: List<System.DiskDetails> = emptyList()
    var selectedShareType: ShareType? = null
    var cpuData: List<System.ReportingGraphResponse>? = null
    var memoryData: List<System.ReportingGraphResponse>? = null
    var temperatureData: List<System.ReportingGraphResponse>? = null
    var initialMetricType: MetricType = MetricType.ALL
    var selectedAppValues : AppConfigPageValues = AppConfigPageValues()
    var selectedService: System.ServiceQueryResponse? = null

}
