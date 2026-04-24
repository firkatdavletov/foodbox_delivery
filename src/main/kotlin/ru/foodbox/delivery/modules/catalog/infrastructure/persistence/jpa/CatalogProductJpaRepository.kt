package ru.foodbox.delivery.modules.catalog.infrastructure.persistence.jpa

import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Query
import ru.foodbox.delivery.modules.catalog.infrastructure.persistence.entity.CatalogProductEntity
import java.util.UUID

interface CatalogProductJpaRepository : JpaRepository<CatalogProductEntity, UUID> {
    @Query(
        """
        select count(p)
        from CatalogProductEntity p
        where p.isActive = true
          and not exists (
              select pi.id
              from CatalogProductImageEntity pi
              where pi.productId = p.id
          )
          and not exists (
              select v.id
              from CatalogProductVariantEntity v
              where v.productId = p.id
                and exists (
                    select vi.id
                    from CatalogProductVariantImageEntity vi
                    where vi.variantId = v.id
                )
          )
        """
    )
    fun countActiveProductsWithoutImages(): Long

    fun findAllByIsActiveTrueOrderByCreatedAtDesc(): List<CatalogProductEntity>
    fun findAllByIsActiveOrderByCreatedAtDesc(isActive: Boolean): List<CatalogProductEntity>

    fun findAllByIsActiveTrueAndCategoryIdOrderByCreatedAtDesc(categoryId: UUID): List<CatalogProductEntity>

    fun findByIdAndIsActiveTrue(id: UUID): CatalogProductEntity?
    fun findByExternalId(externalId: String): CatalogProductEntity?
    fun findBySku(sku: String): CatalogProductEntity?
    fun findBySlug(slug: String): CatalogProductEntity?
    fun findAllByExternalIdIn(externalIds: Collection<String>): List<CatalogProductEntity>
    fun findAllBySlugIn(slugs: Collection<String>): List<CatalogProductEntity>
    fun findAllBySkuIn(skus: Collection<String>): List<CatalogProductEntity>
}
