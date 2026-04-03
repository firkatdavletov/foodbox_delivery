package ru.foodbox.delivery.modules.catalog.domain.repository

import ru.foodbox.delivery.modules.catalog.domain.CatalogProductVariant
import java.util.UUID

interface CatalogProductVariantRepository {
    fun findById(id: UUID): CatalogProductVariant?
    fun findAllByProductId(productId: UUID): List<CatalogProductVariant>
    fun findAllByProductIds(productIds: Collection<UUID>): List<CatalogProductVariant>
    fun findAllBySkuIn(skus: Collection<String>): List<CatalogProductVariant>
    fun deleteAllByProductId(productId: UUID)
    fun saveAll(variants: List<CatalogProductVariant>): List<CatalogProductVariant>
}
