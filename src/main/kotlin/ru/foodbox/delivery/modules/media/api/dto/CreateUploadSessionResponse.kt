package ru.foodbox.delivery.modules.media.api.dto

import ru.foodbox.delivery.modules.media.domain.MediaImageStatus
import ru.foodbox.delivery.modules.media.domain.MediaTargetType
import java.time.Instant
import java.util.UUID

data class CreateUploadSessionResponse(
    val id: UUID,
    val targetType: MediaTargetType,
    val targetId: UUID?,
    val bucket: String,
    val objectKey: String,
    val originalFilename: String,
    val contentType: String,
    val fileSize: Long,
    val status: MediaImageStatus,
    val uploadUrl: String,
    val uploadMethod: String,
    val requiredHeaders: Map<String, String>,
    val expiresAt: Instant,
    val createdAt: Instant,
    val updatedAt: Instant,
)
