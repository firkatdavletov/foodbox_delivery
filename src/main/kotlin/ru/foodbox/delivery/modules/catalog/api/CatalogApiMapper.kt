package ru.foodbox.delivery.modules.catalog.api

import ru.foodbox.delivery.modules.catalog.api.dto.CategoryResponse
import ru.foodbox.delivery.modules.catalog.api.dto.ProductResponse
import ru.foodbox.delivery.modules.catalog.domain.CatalogCategory
import ru.foodbox.delivery.modules.catalog.domain.CatalogProduct

internal fun CatalogCategory.toResponse(): CategoryResponse {
    return CategoryResponse(
        id = id,
        name = name,
        slug = slug,
        imageUrl = imageUrl,
    )
}

internal fun CatalogProduct.toResponse(): ProductResponse {
    return ProductResponse(
        id = id,
        categoryId = categoryId,
        title = title,
        slug = slug,
        description = description,
        priceMinor = priceMinor,
        oldPriceMinor = oldPriceMinor,
        sku = sku,
        imageUrl = imageUrl,
        unit = unit,
        countStep = countStep,
        isActive = isActive,
    )
}
