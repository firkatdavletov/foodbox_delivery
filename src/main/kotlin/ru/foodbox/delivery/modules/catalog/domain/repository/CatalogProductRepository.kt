package ru.foodbox.delivery.modules.catalog.domain.repository

import ru.foodbox.delivery.modules.catalog.domain.CatalogProduct
import java.util.UUID

interface CatalogProductRepository {
    fun findAllActive(categoryId: UUID? = null, query: String? = null): List<CatalogProduct>
    fun findAllByIsActive(isActive: Boolean): List<CatalogProduct>
    fun findById(id: UUID): CatalogProduct?
    fun save(product: CatalogProduct): CatalogProduct
}
