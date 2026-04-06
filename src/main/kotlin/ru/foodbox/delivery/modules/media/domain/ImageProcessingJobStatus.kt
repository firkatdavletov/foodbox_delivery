package ru.foodbox.delivery.modules.media.domain

enum class ImageProcessingJobStatus {
    PENDING,
    PROCESSING,
    COMPLETED,
    RETRY,
    FAILED,
}
