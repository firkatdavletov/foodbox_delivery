package ru.foodbox.delivery.modules.catalog.infrastructure.persistence.jpa

import org.springframework.data.jpa.repository.JpaRepository
import ru.foodbox.delivery.modules.catalog.infrastructure.persistence.entity.CatalogProductVariantEntity
import java.util.UUID

interface CatalogProductVariantJpaRepository : JpaRepository<CatalogProductVariantEntity, UUID> {
    fun findAllByProductIdOrderBySortOrderAscCreatedAtAsc(productId: UUID): List<CatalogProductVariantEntity>
    fun findAllBySkuIn(skus: Collection<String>): List<CatalogProductVariantEntity>
    fun deleteAllByProductId(productId: UUID)
}
