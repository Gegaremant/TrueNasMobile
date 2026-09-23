package com.gegaremant.truenasmobile.data.models

import com.squareup.moshi.JsonClass

object Alerts {
    @JsonClass(generateAdapter = true)
    data class AlertClassesEntry(
        val id: Int,
        val classes: Map<String, AlertClassConfiguration>
    )

    @JsonClass(generateAdapter = true)
    @Suppress("PropertyName")
    data class AlertClassConfiguration(
        val level: AlertLevels,
        val policy: AlertPolicies,
        val proactive_support: Boolean
    )

    enum class AlertLevels {
        INFO,
        NOTICE,
        WARNING,
        ERROR,
        CRITICAL,
        ALERT,
        EMERGENCY
    }

    enum class AlertPolicies {
        IMMEDIATELY,
        HOURLY,
        DAILY,
        NEVER
    }

    @JsonClass(generateAdapter = false)
    @Suppress("PropertyName")
    data class AlertServiceEntry(
        val name: String,
        val attributes: AlertServiceAttributes,
        val level: Alerts.AlertLevels,
        val enabled: Boolean = true,
        val id: Int,
        val type__title: String
    )
    @JsonClass(generateAdapter = false)
    data class AlertServiceCreate(
        val name: String,
        val attributes: AlertServiceAttributes,
        val level: Alerts.AlertLevels,
        val enabled: Boolean = true
    )
    @JsonClass(generateAdapter = false)
    data class AlertServiceUpdate(
        val name: String,
        val attributes: AlertServiceAttributes,
        val level: Alerts.AlertLevels,
        val enabled: Boolean = true
    )

    @JsonClass(generateAdapter = false)
    @Suppress("PropertyName")
    data class AlertServiceAttributes(
        val type: String? = null,
        val region: String? = null,
        val topic_arn: String? = null,
        val aws_access_key_id: String? = null,
        val aws_secret_access_key: String? = null,
        val host: String? = null,
        val username: String? = null,
        val password: String? = null,
        val database: String? = null,
        val series_name: String? = null,
        val email: String? = null,
        val url: String? = null,
        val channel: String? = null,
        val icon_url: String? = null,
        val api_key: String? = null,
        val api_url: String? = null,
        val service_key: String? = null,
        val client_name: String? = null,
        val port: Int? = null,
        val v3: Boolean? = null,
        val community: String? = null,
        val v3_username: String? = null,
        val v3_authkey: String? = null,
        val v3_privkey: String? = null,
        val v3_authprotocol: String? = null,
        val v3_privprotocol: String? = null,
        val bot_token: String? = null,
        val chat_ids: List<Int>? = null,
        val routing_key: String? = null,
        val webhook_url: String? = null,
        val webhook_http_method: String? = null,
        val webhook_http_auth: String? = null,
        val webhook_username: String? = null,
        val webhook_password: String? = null,
        val webhook_headers: List<AlertServiceHeader>? = null
    )

    /** One entry of the `webhook_headers` list on Webhook alert services. */
    @JsonClass(generateAdapter = false)
    data class AlertServiceHeader(
        val name: String,
        val value: String
    )
    /**
     * Request body for alertclasses.update.
     */
    @JsonClass(generateAdapter = false)
    data class AlertClassUpdate(
        val classes: Map<String, Alerts.AlertClassConfiguration>
    )
}
