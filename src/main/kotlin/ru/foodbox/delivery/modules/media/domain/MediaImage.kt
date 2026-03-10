package ru.foodbox.delivery.modules.media.domain

import java.time.Instant
import java.util.UUID

data class MediaImage(
    val id: UUID,
    val targetType: MediaTargetType,
    val targetId: UUID,
    val bucket: String,
    val objectKey: String,
    val originalFilename: String,
    val contentType: String,
    val fileSize: Long,
    val status: MediaImageStatus,
    val publicUrl: String?,
    val createdAt: Instant,
    val updatedAt: Instant,
)
