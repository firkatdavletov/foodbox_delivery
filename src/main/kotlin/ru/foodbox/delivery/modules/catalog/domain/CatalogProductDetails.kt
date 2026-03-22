package ru.foodbox.delivery.modules.catalog.domain

import java.util.UUID

data class CatalogProductDetails(
    val product: CatalogProduct,
    val imageIds: List<UUID> = emptyList(),
    val optionGroups: List<CatalogProductOptionGroupDetails>,
    val defaultVariantId: UUID?,
    val variants: List<CatalogProductVariantDetails>,
)

data class CatalogProductOptionGroupDetails(
    val id: UUID,
    val code: String,
    val title: String,
    val sortOrder: Int,
    val values: List<CatalogProductOptionValueDetails>,
)

data class CatalogProductOptionValueDetails(
    val id: UUID,
    val code: String,
    val title: String,
    val sortOrder: Int,
)

data class CatalogProductVariantDetails(
    val id: UUID,
    val externalId: String?,
    val sku: String,
    val title: String?,
    val priceMinor: Long?,
    val oldPriceMinor: Long?,
    val imageIds: List<UUID> = emptyList(),
    val imageUrls: List<String> = emptyList(),
    val sortOrder: Int,
    val isActive: Boolean,
    val optionValueIds: List<UUID>,
)
