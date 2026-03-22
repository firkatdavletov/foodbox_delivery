package ru.foodbox.delivery.modules.catalog.domain.repository

import ru.foodbox.delivery.modules.catalog.domain.CatalogProductImage
import java.util.UUID

interface CatalogProductImageRepository {
    fun findAllByProductIds(productIds: Collection<UUID>): List<CatalogProductImage>
    fun findAllByImageIds(imageIds: Collection<UUID>): List<CatalogProductImage>
    fun saveAll(images: List<CatalogProductImage>): List<CatalogProductImage>
    fun deleteAllByProductIds(productIds: Collection<UUID>)
}
