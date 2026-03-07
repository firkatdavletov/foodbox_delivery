package ru.foodbox.delivery.schedulers

import org.springframework.scheduling.annotation.Scheduled
import org.springframework.stereotype.Component
import ru.foodbox.delivery.services.ImageUploadService

@Component
class ImageUploadCleanUpScheduler(
    private val imageUploadService: ImageUploadService
) {
    @Scheduled(fixedDelay = 3600000) // раз в час
    fun cleanupStaleUploads() {
        val updatedCount = imageUploadService.failStaleUploads()
        println("[IMAGE CLEANUP] Зависших загрузок переведено в FAILED: $updatedCount")
    }
}
