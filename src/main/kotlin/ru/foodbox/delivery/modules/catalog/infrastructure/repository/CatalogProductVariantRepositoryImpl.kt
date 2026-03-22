package ru.foodbox.delivery.modules.catalog.infrastructure.repository

import org.springframework.stereotype.Repository
import ru.foodbox.delivery.modules.catalog.domain.CatalogProductVariant
import ru.foodbox.delivery.modules.catalog.domain.repository.CatalogProductVariantRepository
import ru.foodbox.delivery.modules.catalog.infrastructure.persistence.entity.CatalogProductVariantEntity
import ru.foodbox.delivery.modules.catalog.infrastructure.persistence.jpa.CatalogProductVariantJpaRepository
import java.util.UUID
import kotlin.jvm.optionals.getOrNull

@Repository
class CatalogProductVariantRepositoryImpl(
    private val jpaRepository: CatalogProductVariantJpaRepository,
) : CatalogProductVariantRepository {

    override fun findById(id: UUID): CatalogProductVariant? {
        val entity = jpaRepository.findById(id).getOrNull() ?: return null
        return toDomain(entity)
    }

    override fun findAllByProductId(productId: UUID): List<CatalogProductVariant> {
        return jpaRepository.findAllByProductIdOrderBySortOrderAscCreatedAtAsc(productId).map(::toDomain)
    }

    override fun findAllBySkuIn(skus: Collection<String>): List<CatalogProductVariant> {
        if (skus.isEmpty()) {
            return emptyList()
        }
        return jpaRepository.findAllBySkuIn(skus).map(::toDomain)
    }

    override fun deleteAllByProductId(productId: UUID) {
        jpaRepository.deleteAllByProductId(productId)
        jpaRepository.flush()
    }

    override fun saveAll(variants: List<CatalogProductVariant>): List<CatalogProductVariant> {
        if (variants.isEmpty()) {
            return emptyList()
        }

        return jpaRepository.saveAll(
            variants.map { variant ->
                CatalogProductVariantEntity(
                    id = variant.id,
                    productId = variant.productId,
                    externalId = variant.externalId,
                    sku = variant.sku,
                    title = variant.title,
                    priceMinor = variant.priceMinor,
                    oldPriceMinor = variant.oldPriceMinor,
                    sortOrder = variant.sortOrder,
                    isActive = variant.isActive,
                    createdAt = variant.createdAt,
                    updatedAt = variant.updatedAt,
                )
            }
        ).map(::toDomain)
    }

    private fun toDomain(entity: CatalogProductVariantEntity): CatalogProductVariant {
        return CatalogProductVariant(
            id = entity.id,
            productId = entity.productId,
            externalId = entity.externalId,
            sku = entity.sku,
            title = entity.title,
            priceMinor = entity.priceMinor,
            oldPriceMinor = entity.oldPriceMinor,
            imageUrls = emptyList(),
            sortOrder = entity.sortOrder,
            isActive = entity.isActive,
            createdAt = entity.createdAt,
            updatedAt = entity.updatedAt,
        )
    }
}
