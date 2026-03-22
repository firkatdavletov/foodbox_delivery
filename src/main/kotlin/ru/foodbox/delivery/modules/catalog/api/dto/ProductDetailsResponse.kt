package ru.foodbox.delivery.modules.catalog.api.dto

import ru.foodbox.delivery.modules.catalog.domain.ProductUnit
import java.util.UUID

data class ProductDetailsResponse(
    val id: UUID,
    val categoryId: UUID,
    val title: String,
    val slug: String,
    val description: String?,
    val priceMinor: Long,
    val oldPriceMinor: Long?,
    val sku: String?,
    val imageUrls: List<String>,
    val unit: ProductUnit,
    val countStep: Int,
    val isActive: Boolean,
    val optionGroups: List<ProductOptionGroupResponse> = emptyList(),
    val defaultVariantId: UUID? = null,
    val variants: List<ProductVariantResponse> = emptyList(),
)

data class ProductOptionGroupResponse(
    val id: UUID,
    val code: String,
    val title: String,
    val sortOrder: Int,
    val values: List<ProductOptionValueResponse>,
)

data class ProductOptionValueResponse(
    val id: UUID,
    val code: String,
    val title: String,
    val sortOrder: Int,
)

data class ProductVariantResponse(
    val id: UUID,
    val externalId: String?,
    val sku: String,
    val title: String?,
    val priceMinor: Long?,
    val oldPriceMinor: Long?,
    val imageUrls: List<String>,
    val sortOrder: Int,
    val isActive: Boolean,
    val optionValueIds: List<UUID>,
)
