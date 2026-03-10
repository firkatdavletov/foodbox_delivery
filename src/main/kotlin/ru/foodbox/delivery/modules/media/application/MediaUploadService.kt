package ru.foodbox.delivery.modules.media.application

import ru.foodbox.delivery.modules.media.application.command.CreateUploadSessionCommand
import ru.foodbox.delivery.modules.media.domain.MediaImage
import ru.foodbox.delivery.modules.media.domain.MediaUploadSession
import java.util.UUID

interface MediaUploadService {
    fun createUploadSession(command: CreateUploadSessionCommand): MediaUploadSession
    fun completeUpload(uploadId: UUID): MediaImage
}
