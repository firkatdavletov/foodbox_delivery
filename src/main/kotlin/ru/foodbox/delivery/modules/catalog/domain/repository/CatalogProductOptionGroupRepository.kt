package ru.foodbox.delivery.modules.catalog.domain.repository

import ru.foodbox.delivery.modules.catalog.domain.CatalogProductOptionGroup
import java.util.UUID

interface CatalogProductOptionGroupRepository {
    fun findAllByProductId(productId: UUID): List<CatalogProductOptionGroup>
    fun deleteAllByProductId(productId: UUID)
    fun saveAll(optionGroups: List<CatalogProductOptionGroup>): List<CatalogProductOptionGroup>
}
