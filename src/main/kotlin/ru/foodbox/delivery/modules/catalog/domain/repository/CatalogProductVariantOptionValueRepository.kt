package ru.foodbox.delivery.modules.catalog.domain.repository

import ru.foodbox.delivery.modules.catalog.domain.CatalogProductVariantOptionValue
import java.util.UUID

interface CatalogProductVariantOptionValueRepository {
    fun findAllByVariantIds(variantIds: Collection<UUID>): List<CatalogProductVariantOptionValue>
    fun deleteAllByVariantIds(variantIds: Collection<UUID>)
    fun saveAll(links: List<CatalogProductVariantOptionValue>): List<CatalogProductVariantOptionValue>
}
