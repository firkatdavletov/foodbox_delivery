package ru.foodbox.delivery.modules.catalog.infrastructure.persistence.jpa

import org.springframework.data.jpa.repository.JpaRepository
import ru.foodbox.delivery.modules.catalog.infrastructure.persistence.entity.CatalogProductOptionGroupEntity
import java.util.UUID

interface CatalogProductOptionGroupJpaRepository : JpaRepository<CatalogProductOptionGroupEntity, UUID> {
    fun findAllByProductIdOrderBySortOrderAscIdAsc(productId: UUID): List<CatalogProductOptionGroupEntity>
    fun deleteAllByProductId(productId: UUID)
}
