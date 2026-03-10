package ru.foodbox.delivery.modules.media.application

import org.springframework.boot.context.properties.ConfigurationProperties
import org.springframework.stereotype.Component

@Component
@ConfigurationProperties(prefix = "media.upload")
class MediaUploadProperties {
    var allowedContentTypes: List<String> = listOf("image/jpeg", "image/png", "image/webp")
    var maxFileSizeBytes: Long = 10L * 1024L * 1024L
    var presignDurationMinutes: Long = 15
}
