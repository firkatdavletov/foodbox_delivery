package ru.foodbox.delivery.modules.catalog.domain.repository

import ru.foodbox.delivery.modules.catalog.domain.CatalogCategory
import java.util.UUID

interface CatalogCategoryRepository {
    fun findAll(activeOnly: Boolean): List<CatalogCategory>
    fun findAllByIsActive(isActive: Boolean): List<CatalogCategory>
    fun findById(id: UUID): CatalogCategory?
    fun findByExternalId(externalId: String): CatalogCategory?
    fun findBySlug(slug: String): CatalogCategory?
    fun findAllByExternalIdIn(externalIds: Collection<String>): List<CatalogCategory>
    fun findAllBySlugIn(slugs: Collection<String>): List<CatalogCategory>
    fun save(category: CatalogCategory): CatalogCategory
}
