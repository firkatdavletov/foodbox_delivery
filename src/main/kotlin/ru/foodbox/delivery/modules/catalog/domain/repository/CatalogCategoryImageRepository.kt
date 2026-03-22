package ru.foodbox.delivery.modules.catalog.domain.repository

import ru.foodbox.delivery.modules.catalog.domain.CatalogCategoryImage
import java.util.UUID

interface CatalogCategoryImageRepository {
    fun findAllByCategoryIds(categoryIds: Collection<UUID>): List<CatalogCategoryImage>
    fun findAllByImageIds(imageIds: Collection<UUID>): List<CatalogCategoryImage>
    fun saveAll(images: List<CatalogCategoryImage>): List<CatalogCategoryImage>
    fun deleteAllByCategoryIds(categoryIds: Collection<UUID>)
}
