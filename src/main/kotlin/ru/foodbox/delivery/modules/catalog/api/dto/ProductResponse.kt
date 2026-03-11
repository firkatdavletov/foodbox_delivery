package ru.foodbox.delivery.modules.catalog.api.dto

import ru.foodbox.delivery.modules.catalog.domain.ProductUnit
import java.util.UUID

data class ProductResponse(
    val id: UUID,
    val categoryId: UUID,
    val title: String,
    val slug: String,
    val description: String?,
    val priceMinor: Long,
    val oldPriceMinor: Long?,
    val sku: String?,
    val imageUrl: String?,
    val unit: ProductUnit,
    val countStep: Int,
    val isActive: Boolean,
)
