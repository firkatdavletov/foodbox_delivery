package ru.foodbox.delivery.modules.media.infrastructure.persistence.jpa

import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Modifying
import org.springframework.data.jpa.repository.Query
import ru.foodbox.delivery.modules.media.domain.ImageProcessingJobStatus
import ru.foodbox.delivery.modules.media.infrastructure.persistence.entity.ImageProcessingJobEntity
import java.time.Instant
import java.util.UUID

interface ImageProcessingJobJpaRepository : JpaRepository<ImageProcessingJobEntity, UUID> {

    fun findByImageId(imageId: UUID): ImageProcessingJobEntity?

    @Query(
        """
        SELECT j FROM ImageProcessingJobEntity j
        WHERE j.status IN :statuses
          AND (j.nextRetryAt IS NULL OR j.nextRetryAt <= :now)
        ORDER BY j.createdAt ASC
        """,
    )
    fun findPendingJobs(
        statuses: Collection<ImageProcessingJobStatus>,
        now: Instant,
    ): List<ImageProcessingJobEntity>

    @Modifying
    @Query(
        """
        UPDATE ImageProcessingJobEntity j
        SET j.status = :targetStatus, j.updatedAt = :now
        WHERE j.id = :id AND j.status = :expectedStatus
        """,
    )
    fun claimJob(
        id: UUID,
        expectedStatus: ImageProcessingJobStatus,
        targetStatus: ImageProcessingJobStatus,
        now: Instant,
    ): Int
}
