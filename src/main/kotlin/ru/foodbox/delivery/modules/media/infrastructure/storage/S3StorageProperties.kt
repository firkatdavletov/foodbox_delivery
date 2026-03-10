package ru.foodbox.delivery.modules.media.infrastructure.storage

import org.springframework.boot.context.properties.ConfigurationProperties
import org.springframework.stereotype.Component

@Component
@ConfigurationProperties(prefix = "s3")
class S3StorageProperties {
    var endpoint: String = "https://storage.yandexcloud.net"
    var region: String = "ru-central1"
    var bucket: String = ""
    var accessKey: String = ""
    var secretKey: String = ""
    var publicBaseUrl: String = ""
    var pathStyleAccess: Boolean = true

    fun isConfigured(): Boolean {
        return endpoint.isNotBlank() &&
            region.isNotBlank() &&
            bucket.isNotBlank() &&
            accessKey.isNotBlank() &&
            secretKey.isNotBlank()
    }
}
