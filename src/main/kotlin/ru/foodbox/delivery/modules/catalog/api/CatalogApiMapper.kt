package ru.foodbox.delivery.modules.catalog.api

import ru.foodbox.delivery.modules.catalog.api.dto.CategoryResponse
import ru.foodbox.delivery.modules.catalog.api.dto.ProductDetailsResponse
import ru.foodbox.delivery.modules.catalog.api.dto.ProductOptionGroupResponse
import ru.foodbox.delivery.modules.catalog.api.dto.ProductOptionValueResponse
import ru.foodbox.delivery.modules.catalog.api.dto.ProductResponse
import ru.foodbox.delivery.modules.catalog.api.dto.ProductVariantResponse
import ru.foodbox.delivery.modules.catalog.domain.CatalogCategory
import ru.foodbox.delivery.modules.catalog.domain.CatalogProductDetails
import ru.foodbox.delivery.modules.catalog.domain.CatalogProduct

internal fun CatalogCategory.toResponse(): CategoryResponse {
    return CategoryResponse(
        id = id,
        name = name,
        slug = slug,
        imageUrls = imageUrls,
        isActive = isActive,
    )
}

internal fun CatalogProduct.toResponse(): ProductResponse {
    return ProductResponse(
        id = id,
        categoryId = categoryId,
        title = title,
        slug = slug,
        description = description,
        brand = brand,
        priceMinor = priceMinor,
        oldPriceMinor = oldPriceMinor,
        sku = sku,
        imageUrls = imageUrls,
        unit = unit,
        countStep = countStep,
        isActive = isActive,
    )
}

internal fun CatalogProductDetails.toDetailsResponse(): ProductDetailsResponse {
    return ProductDetailsResponse(
        id = product.id,
        categoryId = product.categoryId,
        title = product.title,
        slug = product.slug,
        description = product.description,
        priceMinor = product.priceMinor,
        oldPriceMinor = product.oldPriceMinor,
        sku = product.sku,
        imageUrls = product.imageUrls,
        unit = product.unit,
        countStep = product.countStep,
        isActive = product.isActive,
        optionGroups = optionGroups.map { optionGroup ->
            ProductOptionGroupResponse(
                id = optionGroup.id,
                code = optionGroup.code,
                title = optionGroup.title,
                sortOrder = optionGroup.sortOrder,
                values = optionGroup.values.map { optionValue ->
                    ProductOptionValueResponse(
                        id = optionValue.id,
                        code = optionValue.code,
                        title = optionValue.title,
                        sortOrder = optionValue.sortOrder,
                    )
                },
            )
        },
        defaultVariantId = defaultVariantId,
        variants = variants.map { variant ->
            ProductVariantResponse(
                id = variant.id,
                externalId = variant.externalId,
                sku = variant.sku,
                title = variant.title,
                priceMinor = variant.priceMinor,
                oldPriceMinor = variant.oldPriceMinor,
                imageUrls = variant.imageUrls,
                sortOrder = variant.sortOrder,
                isActive = variant.isActive,
                optionValueIds = variant.optionValueIds,
            )
        },
    )
}
