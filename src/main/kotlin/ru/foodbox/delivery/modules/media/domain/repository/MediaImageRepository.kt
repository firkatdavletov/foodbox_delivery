package ru.foodbox.delivery.modules.media.domain.repository

import ru.foodbox.delivery.modules.media.domain.MediaImage
import ru.foodbox.delivery.modules.media.domain.MediaTargetType
import java.util.UUID

interface MediaImageRepository {
    fun findById(id: UUID): MediaImage?
    fun findAllByIds(ids: Collection<UUID>): List<MediaImage>
    fun findAllByTargetTypeAndTargetIdIn(targetType: MediaTargetType, targetIds: Collection<UUID>): List<MediaImage>
    fun save(mediaImage: MediaImage): MediaImage
    fun saveAll(mediaImages: List<MediaImage>): List<MediaImage>
}
