package ru.foodbox.delivery.modules.media.infrastructure.persistence.jpa

import org.springframework.data.jpa.repository.JpaRepository
import ru.foodbox.delivery.modules.media.infrastructure.persistence.entity.MediaImageEntity
import java.util.UUID

interface MediaImageJpaRepository : JpaRepository<MediaImageEntity, UUID> {
    fun findAllByIdIn(ids: Collection<UUID>): List<MediaImageEntity>
}
