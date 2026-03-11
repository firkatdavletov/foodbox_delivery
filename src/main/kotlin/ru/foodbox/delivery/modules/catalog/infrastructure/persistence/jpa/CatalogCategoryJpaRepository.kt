package ru.foodbox.delivery.modules.catalog.infrastructure.persistence.jpa

import org.springframework.data.jpa.repository.JpaRepository
import ru.foodbox.delivery.modules.catalog.infrastructure.persistence.entity.CatalogCategoryEntity
import java.util.UUID

interface CatalogCategoryJpaRepository : JpaRepository<CatalogCategoryEntity, UUID> {
    fun findAllByIsActiveTrueOrderByNameAsc(): List<CatalogCategoryEntity>
    fun findAllByIsActiveOrderByNameAsc(isActive: Boolean): List<CatalogCategoryEntity>
    fun findBySlug(slug: String): CatalogCategoryEntity?
}
