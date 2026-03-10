package ru.foodbox.delivery.modules.media.infrastructure.persistence.entity

import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.EnumType
import jakarta.persistence.Enumerated
import jakarta.persistence.Id
import jakarta.persistence.Index
import jakarta.persistence.Table
import ru.foodbox.delivery.modules.media.domain.MediaImageStatus
import ru.foodbox.delivery.modules.media.domain.MediaTargetType
import java.time.Instant
import java.util.UUID

@Entity
@Table(
    name = "media_images",
    indexes = [
        Index(name = "idx_media_images_target", columnList = "target_type,target_id"),
        Index(name = "idx_media_images_status", columnList = "status"),
    ],
)
class MediaImageEntity(
    @Id
    @Column(nullable = false)
    var id: UUID,

    @Enumerated(EnumType.STRING)
    @Column(name = "target_type", nullable = false, length = 32)
    var targetType: MediaTargetType,

    @Column(name = "target_id", nullable = false)
    var targetId: UUID,

    @Column(nullable = false, length = 255)
    var bucket: String,

    @Column(name = "object_key", nullable = false, length = 512, unique = true)
    var objectKey: String,

    @Column(name = "original_filename", nullable = false, length = 255)
    var originalFilename: String,

    @Column(name = "content_type", nullable = false, length = 255)
    var contentType: String,

    @Column(name = "file_size", nullable = false)
    var fileSize: Long,

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 32)
    var status: MediaImageStatus,

    @Column(name = "public_url", length = 1024)
    var publicUrl: String? = null,

    @Column(name = "created_at", nullable = false)
    var createdAt: Instant,

    @Column(name = "updated_at", nullable = false)
    var updatedAt: Instant,
)
