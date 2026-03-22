package ru.foodbox.delivery.modules.catalog.domain.repository

import ru.foodbox.delivery.modules.catalog.domain.CatalogProductVariantImage
import java.util.UUID

interface CatalogProductVariantImageRepository {
    fun findAllByVariantIds(variantIds: Collection<UUID>): List<CatalogProductVariantImage>
    fun findAllByImageIds(imageIds: Collection<UUID>): List<CatalogProductVariantImage>
    fun saveAll(images: List<CatalogProductVariantImage>): List<CatalogProductVariantImage>
    fun deleteAllByVariantIds(variantIds: Collection<UUID>)
}
