package ru.foodbox.delivery.modules.media.application.command

import ru.foodbox.delivery.modules.media.domain.MediaTargetType
import java.util.UUID

data class CreateUploadSessionCommand(
    val targetType: MediaTargetType,
    val targetId: UUID,
    val originalFilename: String,
    val contentType: String,
    val fileSize: Long,
)
