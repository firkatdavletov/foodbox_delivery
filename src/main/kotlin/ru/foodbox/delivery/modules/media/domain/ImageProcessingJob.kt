package ru.foodbox.delivery.modules.media.domain

import java.time.Instant
import java.util.UUID

data class ImageProcessingJob(
    val id: UUID,
    val imageId: UUID,
    val status: ImageProcessingJobStatus,
    val attempts: Int,
    val maxAttempts: Int,
    val nextRetryAt: Instant?,
    val lastError: String?,
    val createdAt: Instant,
    val updatedAt: Instant,
)
