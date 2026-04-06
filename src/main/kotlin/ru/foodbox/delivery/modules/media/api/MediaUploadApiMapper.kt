package ru.foodbox.delivery.modules.media.api

import ru.foodbox.delivery.modules.media.api.dto.CreateUploadSessionRequest
import ru.foodbox.delivery.modules.media.api.dto.CreateUploadSessionResponse
import ru.foodbox.delivery.modules.media.api.dto.MediaImageResponse
import ru.foodbox.delivery.modules.media.application.command.CreateUploadSessionCommand
import ru.foodbox.delivery.modules.media.domain.MediaImage
import ru.foodbox.delivery.modules.media.domain.MediaUploadSession

internal fun CreateUploadSessionRequest.toCommand(): CreateUploadSessionCommand {
    return CreateUploadSessionCommand(
        targetType = requireNotNull(targetType) { "targetType is required" },
        targetId = targetId,
        originalFilename = requireNotNull(originalFilename) { "originalFilename is required" },
        contentType = requireNotNull(contentType) { "contentType is required" },
        fileSize = requireNotNull(fileSize) { "fileSize is required" },
    )
}

internal fun MediaUploadSession.toCreateResponse(): CreateUploadSessionResponse {
    return CreateUploadSessionResponse(
        id = mediaImage.id,
        targetType = mediaImage.targetType,
        targetId = mediaImage.targetId,
        bucket = mediaImage.bucket,
        objectKey = mediaImage.objectKey,
        originalFilename = mediaImage.originalFilename,
        contentType = mediaImage.contentType,
        fileSize = mediaImage.fileSize,
        status = mediaImage.status,
        uploadUrl = uploadUrl,
        uploadMethod = uploadMethod,
        requiredHeaders = requiredHeaders,
        expiresAt = expiresAt,
        createdAt = mediaImage.createdAt,
        updatedAt = mediaImage.updatedAt,
    )
}

internal fun MediaImage.toResponse(publicBaseUrlBuilder: ((String) -> String)? = null): MediaImageResponse {
    return MediaImageResponse(
        id = id,
        targetType = targetType,
        targetId = targetId,
        bucket = bucket,
        objectKey = objectKey,
        originalFilename = originalFilename,
        contentType = contentType,
        fileSize = fileSize,
        status = status,
        publicUrl = publicUrl,
        thumbUrl = thumbKey?.let { publicBaseUrlBuilder?.invoke(it) ?: publicUrl },
        cardUrl = cardKey?.let { publicBaseUrlBuilder?.invoke(it) ?: publicUrl },
        processingError = processingError,
        createdAt = createdAt,
        updatedAt = updatedAt,
    )
}
