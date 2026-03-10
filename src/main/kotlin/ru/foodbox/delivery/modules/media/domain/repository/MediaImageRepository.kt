package ru.foodbox.delivery.modules.media.domain.repository

import ru.foodbox.delivery.modules.media.domain.MediaImage
import java.util.UUID

interface MediaImageRepository {
    fun findById(id: UUID): MediaImage?
    fun save(mediaImage: MediaImage): MediaImage
}
