package ru.foodbox.delivery.modules.media.infrastructure.repository

import org.springframework.stereotype.Repository
import org.springframework.transaction.annotation.Transactional
import ru.foodbox.delivery.modules.media.domain.ImageProcessingJob
import ru.foodbox.delivery.modules.media.domain.ImageProcessingJobStatus
import ru.foodbox.delivery.modules.media.domain.repository.ImageProcessingJobRepository
import ru.foodbox.delivery.modules.media.infrastructure.persistence.entity.ImageProcessingJobEntity
import ru.foodbox.delivery.modules.media.infrastructure.persistence.jpa.ImageProcessingJobJpaRepository
import java.time.Instant
import java.util.UUID
import kotlin.jvm.optionals.getOrNull

@Repository
class ImageProcessingJobRepositoryImpl(
    private val jpaRepository: ImageProcessingJobJpaRepository,
) : ImageProcessingJobRepository {

    override fun save(job: ImageProcessingJob): ImageProcessingJob {
        val existing = jpaRepository.findById(job.id).getOrNull()
        val entity = existing ?: ImageProcessingJobEntity(
            id = job.id,
            imageId = job.imageId,
            status = job.status,
            attempts = job.attempts,
            maxAttempts = job.maxAttempts,
            nextRetryAt = job.nextRetryAt,
            lastError = job.lastError,
            createdAt = job.createdAt,
            updatedAt = job.updatedAt,
        )

        entity.status = job.status
        entity.attempts = job.attempts
        entity.maxAttempts = job.maxAttempts
        entity.nextRetryAt = job.nextRetryAt
        entity.lastError = job.lastError
        entity.updatedAt = job.updatedAt

        return toDomain(jpaRepository.save(entity))
    }

    override fun findById(id: UUID): ImageProcessingJob? {
        return jpaRepository.findById(id).getOrNull()?.let(::toDomain)
    }

    override fun findByImageId(imageId: UUID): ImageProcessingJob? {
        return jpaRepository.findByImageId(imageId)?.let(::toDomain)
    }

    @Transactional
    override fun claimNextPending(now: Instant, batchSize: Int): List<ImageProcessingJob> {
        val claimableStatuses = listOf(ImageProcessingJobStatus.PENDING, ImageProcessingJobStatus.RETRY)
        val candidates = jpaRepository.findPendingJobs(claimableStatuses, now)

        val claimed = mutableListOf<ImageProcessingJob>()
        for (candidate in candidates) {
            if (claimed.size >= batchSize) break

            val updated = jpaRepository.claimJob(
                id = candidate.id,
                expectedStatus = candidate.status,
                targetStatus = ImageProcessingJobStatus.PROCESSING,
                now = now,
            )
            if (updated == 1) {
                claimed += toDomain(candidate).copy(
                    status = ImageProcessingJobStatus.PROCESSING,
                    updatedAt = now,
                )
            }
        }
        return claimed
    }

    private fun toDomain(entity: ImageProcessingJobEntity): ImageProcessingJob {
        return ImageProcessingJob(
            id = entity.id,
            imageId = entity.imageId,
            status = entity.status,
            attempts = entity.attempts,
            maxAttempts = entity.maxAttempts,
            nextRetryAt = entity.nextRetryAt,
            lastError = entity.lastError,
            createdAt = entity.createdAt,
            updatedAt = entity.updatedAt,
        )
    }
}
