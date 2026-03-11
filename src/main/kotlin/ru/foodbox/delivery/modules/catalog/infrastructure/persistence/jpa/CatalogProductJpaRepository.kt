package ru.foodbox.delivery.modules.catalog.infrastructure.persistence.jpa

import org.springframework.data.jpa.repository.JpaRepository
import ru.foodbox.delivery.modules.catalog.infrastructure.persistence.entity.CatalogProductEntity
import java.util.UUID

interface CatalogProductJpaRepository : JpaRepository<CatalogProductEntity, UUID> {
    fun findAllByIsActiveTrueOrderByCreatedAtDesc(): List<CatalogProductEntity>
    fun findAllByIsActiveOrderByCreatedAtDesc(isActive: Boolean): List<CatalogProductEntity>

    fun findAllByIsActiveTrueAndCategoryIdOrderByCreatedAtDesc(categoryId: UUID): List<CatalogProductEntity>

    fun findAllByIsActiveTrueAndTitleContainingIgnoreCaseOrderByCreatedAtDesc(title: String): List<CatalogProductEntity>

    fun findAllByIsActiveTrueAndCategoryIdAndTitleContainingIgnoreCaseOrderByCreatedAtDesc(
        categoryId: UUID,
        title: String,
    ): List<CatalogProductEntity>

    fun findByExternalId(externalId: String): CatalogProductEntity?
    fun findBySku(sku: String): CatalogProductEntity?
    fun findBySlug(slug: String): CatalogProductEntity?
    fun findAllByExternalIdIn(externalIds: Collection<String>): List<CatalogProductEntity>
    fun findAllBySkuIn(skus: Collection<String>): List<CatalogProductEntity>
}
