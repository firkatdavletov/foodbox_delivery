package ru.foodbox.delivery.modules.catalog.infrastructure.persistence.jpa

import org.springframework.data.jpa.repository.JpaRepository
import ru.foodbox.delivery.modules.catalog.infrastructure.persistence.entity.CatalogCategoryImageEntity
import java.util.UUID

interface CatalogCategoryImageJpaRepository : JpaRepository<CatalogCategoryImageEntity, UUID> {
    fun findAllByCategoryIdInOrderByCategoryIdAscSortOrderAscCreatedAtAsc(categoryIds: Collection<UUID>): List<CatalogCategoryImageEntity>
    fun findAllByImageIdIn(imageIds: Collection<UUID>): List<CatalogCategoryImageEntity>
    fun deleteAllByCategoryIdIn(categoryIds: Collection<UUID>)
}
