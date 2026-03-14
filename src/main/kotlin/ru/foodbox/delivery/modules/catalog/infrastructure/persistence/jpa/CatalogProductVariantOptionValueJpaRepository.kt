package ru.foodbox.delivery.modules.catalog.infrastructure.persistence.jpa

import org.springframework.data.jpa.repository.JpaRepository
import ru.foodbox.delivery.modules.catalog.infrastructure.persistence.entity.CatalogProductVariantOptionValueEntity
import java.util.UUID

interface CatalogProductVariantOptionValueJpaRepository : JpaRepository<CatalogProductVariantOptionValueEntity, UUID> {
    fun findAllByVariantIdIn(variantIds: Collection<UUID>): List<CatalogProductVariantOptionValueEntity>
    fun deleteAllByVariantIdIn(variantIds: Collection<UUID>)
}
