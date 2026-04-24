package ru.foodbox.delivery.modules.catalog.domain.repository

import ru.foodbox.delivery.modules.catalog.domain.CatalogProduct
import java.util.UUID

interface CatalogProductRepository {
    fun findAllActive(categoryId: UUID? = null, query: String? = null): List<CatalogProduct>
    fun findAllByIds(ids: Collection<UUID>): List<CatalogProduct>
    fun findAllActiveByIds(ids: Collection<UUID>): List<CatalogProduct>
    fun findAllByIsActive(isActive: Boolean): List<CatalogProduct>
    fun findById(id: UUID): CatalogProduct?
    fun findActiveById(id: UUID): CatalogProduct?
    fun findByExternalId(externalId: String): CatalogProduct?
    fun findBySku(sku: String): CatalogProduct?
    fun findAllByExternalIdIn(externalIds: Collection<String>): List<CatalogProduct>
    fun findAllBySlugIn(slugs: Collection<String>): List<CatalogProduct>
    fun findAllBySkuIn(skus: Collection<String>): List<CatalogProduct>
    fun save(product: CatalogProduct): CatalogProduct
}
