package ru.foodbox.delivery.modules.appconfig.api.response

data class AppConfigResponse(
    val minSupportedVersion: String?,
    val maintenance: Map<String, String>?,
    val featureFlags: Map<String, String>?,
)
