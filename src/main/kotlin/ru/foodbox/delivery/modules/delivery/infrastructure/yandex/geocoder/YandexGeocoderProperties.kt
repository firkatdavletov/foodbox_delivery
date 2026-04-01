package ru.foodbox.delivery.modules.delivery.infrastructure.yandex.geocoder

import org.springframework.boot.context.properties.ConfigurationProperties
import org.springframework.stereotype.Component

@Component
@ConfigurationProperties(prefix = "yandex.geocoder")
class YandexGeocoderProperties {
    var enabled: Boolean = false
    var apiKey: String = ""
    var baseUrl: String = "https://geocode-maps.yandex.ru/v1"
    var lang: String = "ru_RU"
    var kind: String = "house"
    var results: Int = 1
    var connectTimeoutMs: Int = 3_000
    var readTimeoutMs: Int = 10_000

    fun isConfigured(): Boolean {
        return enabled &&
            apiKey.isNotBlank() &&
            baseUrl.isNotBlank()
    }
}
