package ru.foodbox.delivery.modules.media.domain.repository

import ru.foodbox.delivery.modules.media.domain.ImageProcessingJob
import java.time.Instant
import java.util.UUID

interface ImageProcessingJobRepository {
    fun save(job: ImageProcessingJob): ImageProcessingJob
    fun findById(id: UUID): ImageProcessingJob?
    fun findByImageId(imageId: UUID): ImageProcessingJob?
    fun claimNextPending(now: Instant, batchSize: Int): List<ImageProcessingJob>
}
