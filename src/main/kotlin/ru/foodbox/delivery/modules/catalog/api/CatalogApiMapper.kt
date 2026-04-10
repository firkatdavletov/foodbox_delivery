package ru.foodbox.delivery.modules.catalog.api

import ru.foodbox.delivery.modules.catalog.api.dto.CategoryResponse
import ru.foodbox.delivery.modules.catalog.api.dto.AdminCategoryResponse
import ru.foodbox.delivery.modules.catalog.api.dto.AdminProductDetailsResponse
import ru.foodbox.delivery.modules.catalog.api.dto.ProductModifierGroupResponse
import ru.foodbox.delivery.modules.catalog.api.dto.ProductModifierOptionResponse
import ru.foodbox.delivery.modules.catalog.api.dto.AdminProductVariantResponse
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

internal fun CatalogCategory.toAdminResponse(): AdminCategoryResponse {
    return AdminCategoryResponse(
        id = id,
        name = name,
        slug = slug,
        imageIds = imageIds,
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
        isConfigured = isConfigured,
    )
}

internal fun CatalogProductDetails.toPublicDetailsResponse(): ProductDetailsResponse {
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
        isConfigured = product.isConfigured,
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
        modifierGroups = modifierGroups.map { modifierGroup ->
            ProductModifierGroupResponse(
                id = modifierGroup.id,
                code = modifierGroup.code,
                name = modifierGroup.name,
                minSelected = modifierGroup.minSelected,
                maxSelected = modifierGroup.maxSelected,
                isRequired = modifierGroup.isRequired,
                isActive = modifierGroup.isActive,
                sortOrder = modifierGroup.sortOrder,
                options = modifierGroup.options.map { option ->
                    ProductModifierOptionResponse(
                        id = option.id,
                        code = option.code,
                        name = option.name,
                        description = option.description,
                        priceType = option.priceType,
                        price = option.price,
                        applicationScope = option.applicationScope,
                        isDefault = option.isDefault,
                        isActive = option.isActive,
                        sortOrder = option.sortOrder,
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

internal fun CatalogProductDetails.toAdminDetailsResponse(): AdminProductDetailsResponse {
    return AdminProductDetailsResponse(
        id = product.id,
        categoryId = product.categoryId,
        title = product.title,
        slug = product.slug,
        description = product.description,
        priceMinor = product.priceMinor,
        oldPriceMinor = product.oldPriceMinor,
        sku = product.sku,
        imageIds = imageIds,
        imageUrls = product.imageUrls,
        unit = product.unit,
        countStep = product.countStep,
        isActive = product.isActive,
        isConfigured = product.isConfigured,
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
        modifierGroups = modifierGroups.map { modifierGroup ->
            ProductModifierGroupResponse(
                id = modifierGroup.id,
                code = modifierGroup.code,
                name = modifierGroup.name,
                minSelected = modifierGroup.minSelected,
                maxSelected = modifierGroup.maxSelected,
                isRequired = modifierGroup.isRequired,
                isActive = modifierGroup.isActive,
                sortOrder = modifierGroup.sortOrder,
                options = modifierGroup.options.map { option ->
                    ProductModifierOptionResponse(
                        id = option.id,
                        code = option.code,
                        name = option.name,
                        description = option.description,
                        priceType = option.priceType,
                        price = option.price,
                        applicationScope = option.applicationScope,
                        isDefault = option.isDefault,
                        isActive = option.isActive,
                        sortOrder = option.sortOrder,
                    )
                },
            )
        },
        defaultVariantId = defaultVariantId,
        variants = variants.map { variant ->
            AdminProductVariantResponse(
                id = variant.id,
                externalId = variant.externalId,
                sku = variant.sku,
                title = variant.title,
                priceMinor = variant.priceMinor,
                oldPriceMinor = variant.oldPriceMinor,
                imageIds = variant.imageIds,
                imageUrls = variant.imageUrls,
                sortOrder = variant.sortOrder,
                isActive = variant.isActive,
                optionValueIds = variant.optionValueIds,
            )
        },
    )
}
