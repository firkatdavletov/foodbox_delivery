package ru.foodbox.delivery.modules.catalog.api

import ru.foodbox.delivery.modules.catalog.api.dto.CategoryResponse
import ru.foodbox.delivery.modules.catalog.api.dto.AdminCategoryDetailsResponse
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
import ru.foodbox.delivery.modules.catalog.domain.CatalogProductOptionGroupDetails
import ru.foodbox.delivery.modules.catalog.domain.CatalogProductOptionValueDetails
import ru.foodbox.delivery.modules.catalog.domain.CatalogProductVariantDetails

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

internal fun CatalogCategory.toAdminDetailsResponse(): AdminCategoryDetailsResponse {
    return AdminCategoryDetailsResponse(
        id = id,
        externalId = externalId,
        name = name,
        slug = slug,
        parentId = parentId,
        description = description,
        sortOrder = sortOrder,
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
        optionGroups = optionGroups.map(CatalogProductOptionGroupDetails::toResponse),
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
        variants = variants.map(CatalogProductVariantDetails::toResponse),
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
        optionGroups = optionGroups.map(CatalogProductOptionGroupDetails::toResponse),
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
        variants = variants.map(CatalogProductVariantDetails::toAdminResponse),
    )
}

internal fun CatalogProductOptionGroupDetails.toResponse(): ProductOptionGroupResponse {
    return ProductOptionGroupResponse(
        id = id,
        code = code,
        title = title,
        sortOrder = sortOrder,
        values = values.map(CatalogProductOptionValueDetails::toResponse),
    )
}

internal fun CatalogProductOptionValueDetails.toResponse(): ProductOptionValueResponse {
    return ProductOptionValueResponse(
        id = id,
        code = code,
        title = title,
        sortOrder = sortOrder,
    )
}

internal fun CatalogProductVariantDetails.toResponse(): ProductVariantResponse {
    return ProductVariantResponse(
        id = id,
        externalId = externalId,
        sku = sku,
        title = title,
        priceMinor = priceMinor,
        oldPriceMinor = oldPriceMinor,
        imageUrls = imageUrls,
        sortOrder = sortOrder,
        isActive = isActive,
        optionValueIds = optionValueIds,
    )
}

internal fun CatalogProductVariantDetails.toAdminResponse(): AdminProductVariantResponse {
    return AdminProductVariantResponse(
        id = id,
        externalId = externalId,
        sku = sku,
        title = title,
        priceMinor = priceMinor,
        oldPriceMinor = oldPriceMinor,
        imageIds = imageIds,
        imageUrls = imageUrls,
        sortOrder = sortOrder,
        isActive = isActive,
        optionValueIds = optionValueIds,
    )
}
