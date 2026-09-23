package com.imnotndesh.truehub.data.models

import com.squareup.moshi.JsonClass

@JsonClass(generateAdapter = true)
data class QuickLaunchApp(
    val appName: String,
    val title: String,
    val iconUrl: String? = null,
    val webUiUrl: String,
    val cachedIconPath: String? = null
)