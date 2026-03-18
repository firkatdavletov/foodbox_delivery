package ru.foodbox.delivery.modules.virtualtryon.infrastructure.fashn

import org.springframework.boot.context.properties.ConfigurationProperties
import org.springframework.stereotype.Component

@Component
@ConfigurationProperties(prefix = "virtual-try-on.fashn")
class FashnVirtualTryOnProperties {
    var enabled: Boolean = true
    var apiKey: String = ""
    var baseUrl: String = "https://api.fashn.ai"
    var webhookBaseUrl: String = ""
    var webhookSecret: String = ""
    var connectTimeoutMs: Int = 3_000
    var readTimeoutMs: Int = 15_000
}
