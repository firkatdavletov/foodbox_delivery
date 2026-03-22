package ru.foodbox.delivery.modules.catalog.infrastructure.persistence.jpa

import org.springframework.data.jpa.repository.JpaRepository
import ru.foodbox.delivery.modules.catalog.infrastructure.persistence.entity.CatalogProductImageEntity
import java.util.UUID

interface CatalogProductImageJpaRepository : JpaRepository<CatalogProductImageEntity, UUID> {
    fun findAllByProductIdInOrderByProductIdAscSortOrderAscCreatedAtAsc(productIds: Collection<UUID>): List<CatalogProductImageEntity>
    fun findAllByImageIdIn(imageIds: Collection<UUID>): List<CatalogProductImageEntity>
    fun deleteAllByProductIdIn(productIds: Collection<UUID>)
}
