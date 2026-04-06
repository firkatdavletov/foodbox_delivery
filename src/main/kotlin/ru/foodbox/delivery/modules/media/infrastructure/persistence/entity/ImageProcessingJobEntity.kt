package ru.foodbox.delivery.modules.media.infrastructure.persistence.entity

import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.EnumType
import jakarta.persistence.Enumerated
import jakarta.persistence.Id
import jakarta.persistence.Index
import jakarta.persistence.Table
import ru.foodbox.delivery.modules.media.domain.ImageProcessingJobStatus
import java.time.Instant
import java.util.UUID

@Entity
@Table(
    name = "image_processing_jobs",
    indexes = [
        Index(name = "idx_image_processing_jobs_poll", columnList = "status,next_retry_at"),
    ],
)
class ImageProcessingJobEntity(
    @Id
    @Column(nullable = false)
    var id: UUID,

    @Column(name = "image_id", nullable = false, unique = true)
    var imageId: UUID,

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 32)
    var status: ImageProcessingJobStatus,

    @Column(nullable = false)
    var attempts: Int,

    @Column(name = "max_attempts", nullable = false)
    var maxAttempts: Int,

    @Column(name = "next_retry_at")
    var nextRetryAt: Instant? = null,

    @Column(name = "last_error", columnDefinition = "TEXT")
    var lastError: String? = null,

    @Column(name = "created_at", nullable = false)
    var createdAt: Instant,

    @Column(name = "updated_at", nullable = false)
    var updatedAt: Instant,
)
