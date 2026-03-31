package ru.foodbox.delivery.modules.catalog.modifier.infrastructure.repository

import org.springframework.stereotype.Repository
import ru.foodbox.delivery.modules.catalog.modifier.domain.ProductModifierGroup
import ru.foodbox.delivery.modules.catalog.modifier.domain.repository.ProductModifierGroupRepository
import ru.foodbox.delivery.modules.catalog.modifier.infrastructure.persistence.entity.ProductModifierGroupEntity
import ru.foodbox.delivery.modules.catalog.modifier.infrastructure.persistence.jpa.ProductModifierGroupJpaRepository

@Repository
class ProductModifierGroupRepositoryImpl(
    private val jpaRepository: ProductModifierGroupJpaRepository,
) : ProductModifierGroupRepository {

    override fun findAllByProductId(productId: java.util.UUID): List<ProductModifierGroup> {
        return jpaRepository.findAllByProductIdOrderBySortOrderAsc(productId).map(::toDomain)
    }

    override fun findAllByProductIds(productIds: Collection<java.util.UUID>): List<ProductModifierGroup> {
        if (productIds.isEmpty()) {
            return emptyList()
        }
        return jpaRepository.findAllByProductIdIn(productIds)
            .map(::toDomain)
            .sortedWith(compareBy<ProductModifierGroup> { it.productId }.thenBy { it.sortOrder })
    }

    override fun deleteAllByProductId(productId: java.util.UUID) {
        jpaRepository.deleteAllByProductId(productId)
    }

    override fun saveAll(productModifierGroups: List<ProductModifierGroup>): List<ProductModifierGroup> {
        if (productModifierGroups.isEmpty()) {
            return emptyList()
        }
        return jpaRepository.saveAll(productModifierGroups.map(::toEntity)).map(::toDomain)
    }

    private fun toEntity(link: ProductModifierGroup): ProductModifierGroupEntity {
        return ProductModifierGroupEntity(
            id = link.id,
            productId = link.productId,
            modifierGroupId = link.modifierGroupId,
            sortOrder = link.sortOrder,
            isActive = link.isActive,
        )
    }

    private fun toDomain(entity: ProductModifierGroupEntity): ProductModifierGroup {
        return ProductModifierGroup(
            id = entity.id,
            productId = entity.productId,
            modifierGroupId = entity.modifierGroupId,
            sortOrder = entity.sortOrder,
            isActive = entity.isActive,
        )
    }
}
