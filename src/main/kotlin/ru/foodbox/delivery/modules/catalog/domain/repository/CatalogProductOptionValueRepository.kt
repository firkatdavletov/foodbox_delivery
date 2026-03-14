package ru.foodbox.delivery.modules.catalog.domain.repository

import ru.foodbox.delivery.modules.catalog.domain.CatalogProductOptionValue
import java.util.UUID

interface CatalogProductOptionValueRepository {
    fun findAllByOptionGroupIds(optionGroupIds: Collection<UUID>): List<CatalogProductOptionValue>
    fun deleteAllByOptionGroupIds(optionGroupIds: Collection<UUID>)
    fun saveAll(optionValues: List<CatalogProductOptionValue>): List<CatalogProductOptionValue>
}
