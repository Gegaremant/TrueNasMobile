package com.imnotndesh.truehub.ui.services.apps.details.appdetails

import com.imnotndesh.truehub.data.models.Apps
import com.imnotndesh.truehub.data.models.System
import com.imnotndesh.truehub.ui.homepage.details.MetricType
import com.imnotndesh.truehub.ui.homepage.details.ShareType

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
