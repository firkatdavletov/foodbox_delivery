package ru.foodbox.delivery.modules.catalog.infrastructure.repository

import org.springframework.stereotype.Repository
import ru.foodbox.delivery.modules.catalog.domain.CatalogProduct
import ru.foodbox.delivery.modules.catalog.domain.repository.CatalogProductRepository
import ru.foodbox.delivery.modules.catalog.infrastructure.persistence.entity.CatalogProductEntity
import ru.foodbox.delivery.modules.catalog.infrastructure.persistence.jpa.CatalogProductJpaRepository
import java.util.UUID
import kotlin.jvm.optionals.getOrNull

@Repository
class CatalogProductRepositoryImpl(
    private val jpaRepository: CatalogProductJpaRepository,
) : CatalogProductRepository {

    override fun findAllActive(categoryId: UUID?, query: String?): List<CatalogProduct> {
        val entities = when {
            categoryId != null && !query.isNullOrBlank() -> {
                jpaRepository.findAllByIsActiveTrueAndCategoryIdAndTitleContainingIgnoreCaseOrderByCreatedAtDesc(
                    categoryId = categoryId,
                    title = query,
                )
            }
            categoryId != null -> jpaRepository.findAllByIsActiveTrueAndCategoryIdOrderByCreatedAtDesc(categoryId)
            !query.isNullOrBlank() -> jpaRepository.findAllByIsActiveTrueAndTitleContainingIgnoreCaseOrderByCreatedAtDesc(query)
            else -> jpaRepository.findAllByIsActiveTrueOrderByCreatedAtDesc()
        }

        return entities.map(::toDomain)
    }

    override fun findAllByIsActive(isActive: Boolean): List<CatalogProduct> {
        return jpaRepository.findAllByIsActiveOrderByCreatedAtDesc(isActive).map(::toDomain)
    }

    override fun findById(id: UUID): CatalogProduct? {
        val entity = jpaRepository.findById(id).getOrNull() ?: return null
        return toDomain(entity)
    }

    override fun findByExternalId(externalId: String): CatalogProduct? {
        val entity = jpaRepository.findByExternalId(externalId) ?: return null
        return toDomain(entity)
    }

    override fun findBySku(sku: String): CatalogProduct? {
        val entity = jpaRepository.findBySku(sku) ?: return null
        return toDomain(entity)
    }

    override fun findAllByExternalIdIn(externalIds: Collection<String>): List<CatalogProduct> {
        if (externalIds.isEmpty()) {
            return emptyList()
        }
        return jpaRepository.findAllByExternalIdIn(externalIds).map(::toDomain)
    }

    override fun findAllBySkuIn(skus: Collection<String>): List<CatalogProduct> {
        if (skus.isEmpty()) {
            return emptyList()
        }
        return jpaRepository.findAllBySkuIn(skus).map(::toDomain)
    }

    override fun save(product: CatalogProduct): CatalogProduct {
        val existing = jpaRepository.findById(product.id).getOrNull()
        val entity = existing ?: CatalogProductEntity(
            id = product.id,
            externalId = product.externalId,
            categoryId = product.categoryId,
            title = product.title,
            slug = product.slug,
            description = product.description,
            priceMinor = product.priceMinor,
            oldPriceMinor = product.oldPriceMinor,
            sku = product.sku,
            brand = product.brand,
            imageUrl = product.imageUrl,
            sortOrder = product.sortOrder,
            unit = product.unit,
            countStep = product.countStep,
            isActive = product.isActive,
            createdAt = product.createdAt,
            updatedAt = product.updatedAt,
        )

        entity.externalId = product.externalId
        entity.categoryId = product.categoryId
        entity.title = product.title
        entity.slug = product.slug
        entity.description = product.description
        entity.priceMinor = product.priceMinor
        entity.oldPriceMinor = product.oldPriceMinor
        entity.sku = product.sku
        entity.brand = product.brand
        entity.imageUrl = product.imageUrl
        entity.sortOrder = product.sortOrder
        entity.unit = product.unit
        entity.countStep = product.countStep
        entity.isActive = product.isActive
        entity.updatedAt = product.updatedAt

        val saved = jpaRepository.save(entity)
        return toDomain(saved)
    }

    private fun toDomain(entity: CatalogProductEntity): CatalogProduct {
        return CatalogProduct(
            id = entity.id,
            categoryId = entity.categoryId,
            title = entity.title,
            slug = entity.slug,
            description = entity.description,
            priceMinor = entity.priceMinor,
            oldPriceMinor = entity.oldPriceMinor,
            sku = entity.sku,
            imageUrl = entity.imageUrl,
            unit = entity.unit,
            countStep = entity.countStep,
            isActive = entity.isActive,
            createdAt = entity.createdAt,
            updatedAt = entity.updatedAt,
            externalId = entity.externalId,
            brand = entity.brand,
            sortOrder = entity.sortOrder,
        )
    }
}
