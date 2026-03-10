package ru.foodbox.delivery.modules.media.api

import jakarta.validation.Valid
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController
import ru.foodbox.delivery.modules.media.api.dto.CreateUploadSessionRequest
import ru.foodbox.delivery.modules.media.api.dto.CreateUploadSessionResponse
import ru.foodbox.delivery.modules.media.api.dto.MediaImageResponse
import ru.foodbox.delivery.modules.media.application.MediaUploadService
import java.util.UUID

@RestController
@RequestMapping("/api/v1/admin/media/uploads")
class MediaUploadController(
    private val mediaUploadService: MediaUploadService,
) {

    @PostMapping
    fun createUploadSession(
        @Valid @RequestBody request: CreateUploadSessionRequest,
    ): CreateUploadSessionResponse {
        val uploadSession = mediaUploadService.createUploadSession(request.toCommand())
        return uploadSession.toCreateResponse()
    }

    @PostMapping("/{uploadId}/complete")
    fun completeUpload(
        @PathVariable uploadId: UUID,
    ): MediaImageResponse {
        return mediaUploadService.completeUpload(uploadId).toResponse()
    }
}
