package ru.foodbox.delivery.modules.catalog.domain.repository

import ru.foodbox.delivery.modules.catalog.domain.CatalogCategory
import java.util.UUID

interface CatalogCategoryRepository {
    fun findAll(activeOnly: Boolean): List<CatalogCategory>
    fun findAllByIsActive(isActive: Boolean): List<CatalogCategory>
    fun findById(id: UUID): CatalogCategory?
    fun save(category: CatalogCategory): CatalogCategory
}
