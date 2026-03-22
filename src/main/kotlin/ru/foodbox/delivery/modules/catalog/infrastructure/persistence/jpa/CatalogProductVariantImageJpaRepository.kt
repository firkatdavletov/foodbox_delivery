package ru.foodbox.delivery.modules.catalog.infrastructure.persistence.jpa

import org.springframework.data.jpa.repository.JpaRepository
import ru.foodbox.delivery.modules.catalog.infrastructure.persistence.entity.CatalogProductVariantImageEntity
import java.util.UUID

interface CatalogProductVariantImageJpaRepository : JpaRepository<CatalogProductVariantImageEntity, UUID> {
    fun findAllByVariantIdInOrderByVariantIdAscSortOrderAscCreatedAtAsc(variantIds: Collection<UUID>): List<CatalogProductVariantImageEntity>
    fun findAllByImageIdIn(imageIds: Collection<UUID>): List<CatalogProductVariantImageEntity>
    fun deleteAllByVariantIdIn(variantIds: Collection<UUID>)
}
