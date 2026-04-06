package ru.foodbox.delivery.modules.media.application

import org.springframework.boot.context.properties.ConfigurationProperties
import org.springframework.stereotype.Component

@Component
@ConfigurationProperties(prefix = "media.processing")
class ImageProcessingProperties {
    var thumb: ImageSizeProperties = ImageSizeProperties(400, 400, 80)
    var card: ImageSizeProperties = ImageSizeProperties(800, 800, 85)
    var maxAttempts: Int = 3
    var retryDelaySeconds: Long = 30
    var workerPollIntervalMs: Long = 5000
    var workerBatchSize: Int = 5
}

class ImageSizeProperties(
    var width: Int = 400,
    var height: Int = 400,
    var quality: Int = 80,
)
