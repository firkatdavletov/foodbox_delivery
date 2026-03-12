package ru.foodbox.delivery.common.security

import org.springframework.boot.context.properties.ConfigurationProperties

@ConfigurationProperties(prefix = "app.cors")
data class CorsProps(
    var siteAllowedOrigins: List<String> = emptyList(),
    var adminAllowedOrigins: List<String> = emptyList(),
)