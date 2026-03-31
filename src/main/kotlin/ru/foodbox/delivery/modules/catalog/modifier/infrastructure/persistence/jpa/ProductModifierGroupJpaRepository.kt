package ru.foodbox.delivery.modules.catalog.modifier.infrastructure.persistence.jpa

import org.springframework.data.jpa.repository.JpaRepository
import ru.foodbox.delivery.modules.catalog.modifier.infrastructure.persistence.entity.ProductModifierGroupEntity
import java.util.UUID

interface ProductModifierGroupJpaRepository : JpaRepository<ProductModifierGroupEntity, UUID> {
    fun findAllByProductIdOrderBySortOrderAsc(productId: UUID): List<ProductModifierGroupEntity>
    fun findAllByProductIdIn(productIds: Collection<UUID>): List<ProductModifierGroupEntity>
    fun deleteAllByProductId(productId: UUID)
}
