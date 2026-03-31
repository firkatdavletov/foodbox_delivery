package ru.foodbox.delivery.modules.catalog.modifier.domain.repository

import ru.foodbox.delivery.modules.catalog.modifier.domain.ProductModifierGroup
import java.util.UUID

interface ProductModifierGroupRepository {
    fun findAllByProductId(productId: UUID): List<ProductModifierGroup>
    fun findAllByProductIds(productIds: Collection<UUID>): List<ProductModifierGroup>
    fun deleteAllByProductId(productId: UUID)
    fun saveAll(productModifierGroups: List<ProductModifierGroup>): List<ProductModifierGroup>
}
