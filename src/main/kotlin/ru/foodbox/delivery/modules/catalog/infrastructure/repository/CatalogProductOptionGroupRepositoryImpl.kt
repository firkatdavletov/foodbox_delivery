package ru.foodbox.delivery.modules.catalog.infrastructure.repository

import org.springframework.stereotype.Repository
import ru.foodbox.delivery.modules.catalog.domain.CatalogProductOptionGroup
import ru.foodbox.delivery.modules.catalog.domain.repository.CatalogProductOptionGroupRepository
import ru.foodbox.delivery.modules.catalog.infrastructure.persistence.entity.CatalogProductOptionGroupEntity
import ru.foodbox.delivery.modules.catalog.infrastructure.persistence.jpa.CatalogProductOptionGroupJpaRepository
import java.util.UUID

@Repository
class CatalogProductOptionGroupRepositoryImpl(
    private val jpaRepository: CatalogProductOptionGroupJpaRepository,
) : CatalogProductOptionGroupRepository {

    override fun findAllByProductId(productId: UUID): List<CatalogProductOptionGroup> {
        return jpaRepository.findAllByProductIdOrderBySortOrderAscIdAsc(productId).map(::toDomain)
    }

    override fun deleteAllByProductId(productId: UUID) {
        jpaRepository.deleteAllByProductId(productId)
        jpaRepository.flush()
    }

    override fun saveAll(optionGroups: List<CatalogProductOptionGroup>): List<CatalogProductOptionGroup> {
        if (optionGroups.isEmpty()) {
            return emptyList()
        }

        return jpaRepository.saveAll(
            optionGroups.map { optionGroup ->
                CatalogProductOptionGroupEntity(
                    id = optionGroup.id,
                    productId = optionGroup.productId,
                    code = optionGroup.code,
                    title = optionGroup.title,
                    sortOrder = optionGroup.sortOrder,
                )
            }
        ).map(::toDomain)
    }

    private fun toDomain(entity: CatalogProductOptionGroupEntity): CatalogProductOptionGroup {
        return CatalogProductOptionGroup(
            id = entity.id,
            productId = entity.productId,
            code = entity.code,
            title = entity.title,
            sortOrder = entity.sortOrder,
        )
    }
}
