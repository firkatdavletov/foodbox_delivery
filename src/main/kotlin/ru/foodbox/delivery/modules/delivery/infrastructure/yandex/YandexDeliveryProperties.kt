package ru.foodbox.delivery.modules.delivery.infrastructure.yandex

import org.springframework.boot.context.properties.ConfigurationProperties
import org.springframework.stereotype.Component

@Component
@ConfigurationProperties(prefix = "yandex.delivery")
class YandexDeliveryProperties {
    var enabled: Boolean = false
    var token: String = ""
    var baseUrl: String = "https://b2b-authproxy.taxi.yandex.net"
    var sourceStationId: String = ""
    var sourcePickupIntervalHours: Long = 24
    var defaultPlaceWeightGrams: Long = 1_000
    var defaultPlaceLengthCm: Int = 10
    var defaultPlaceHeightCm: Int = 10
    var defaultPlaceWidthCm: Int = 10
    var connectTimeoutMs: Int = 3_000
    var readTimeoutMs: Int = 10_000

    fun isConfigured(): Boolean {
        return enabled &&
            token.isNotBlank() &&
            baseUrl.isNotBlank() &&
            sourceStationId.isNotBlank()
    }
}
