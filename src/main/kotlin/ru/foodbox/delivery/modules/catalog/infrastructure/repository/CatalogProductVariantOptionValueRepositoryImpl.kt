package ru.foodbox.delivery.modules.catalog.infrastructure.repository

import org.springframework.stereotype.Repository
import ru.foodbox.delivery.modules.catalog.domain.CatalogProductVariantOptionValue
import ru.foodbox.delivery.modules.catalog.domain.repository.CatalogProductVariantOptionValueRepository
import ru.foodbox.delivery.modules.catalog.infrastructure.persistence.entity.CatalogProductVariantOptionValueEntity
import ru.foodbox.delivery.modules.catalog.infrastructure.persistence.jpa.CatalogProductVariantOptionValueJpaRepository
import java.util.UUID

@Repository
class CatalogProductVariantOptionValueRepositoryImpl(
    private val jpaRepository: CatalogProductVariantOptionValueJpaRepository,
) : CatalogProductVariantOptionValueRepository {

    override fun findAllByVariantIds(variantIds: Collection<UUID>): List<CatalogProductVariantOptionValue> {
        if (variantIds.isEmpty()) {
            return emptyList()
        }

        return jpaRepository.findAllByVariantIdIn(variantIds).map(::toDomain)
    }

    override fun deleteAllByVariantIds(variantIds: Collection<UUID>) {
        if (variantIds.isEmpty()) {
            return
        }

        jpaRepository.deleteAllByVariantIdIn(variantIds)
        jpaRepository.flush()
    }

    override fun saveAll(links: List<CatalogProductVariantOptionValue>): List<CatalogProductVariantOptionValue> {
        if (links.isEmpty()) {
            return emptyList()
        }

        return jpaRepository.saveAll(
            links.map { link ->
                CatalogProductVariantOptionValueEntity(
                    id = link.id,
                    variantId = link.variantId,
                    optionGroupId = link.optionGroupId,
                    optionValueId = link.optionValueId,
                )
            }
        ).map(::toDomain)
    }

    private fun toDomain(entity: CatalogProductVariantOptionValueEntity): CatalogProductVariantOptionValue {
        return CatalogProductVariantOptionValue(
            id = entity.id,
            variantId = entity.variantId,
            optionGroupId = entity.optionGroupId,
            optionValueId = entity.optionValueId,
        )
    }
}
