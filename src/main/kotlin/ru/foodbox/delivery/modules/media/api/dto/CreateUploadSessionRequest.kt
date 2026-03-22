package ru.foodbox.delivery.modules.media.api.dto

import jakarta.validation.constraints.NotBlank
import jakarta.validation.constraints.Positive
import jakarta.validation.constraints.Size
import ru.foodbox.delivery.modules.media.domain.MediaTargetType
import java.util.UUID

data class CreateUploadSessionRequest(
    val targetType: MediaTargetType?,

    val targetId: UUID?,

    @field:NotBlank
    @field:Size(max = 255)
    val originalFilename: String?,

    @field:NotBlank
    @field:Size(max = 255)
    val contentType: String?,

    @field:Positive
    val fileSize: Long?,
)
