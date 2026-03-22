package ru.foodbox.delivery.modules.catalog.domain

import java.time.Instant
import java.util.UUID

data class CatalogProduct(
    val id: UUID,
    val categoryId: UUID,
    val title: String,
    val slug: String,
    val description: String?,
    val priceMinor: Long,
    val oldPriceMinor: Long?,
    val sku: String?,
    val imageUrls: List<String> = emptyList(),
    val unit: ProductUnit,
    val countStep: Int,
    val isActive: Boolean,
    val createdAt: Instant,
    val updatedAt: Instant,
    val externalId: String? = null,
    val brand: String? = null,
    val sortOrder: Int = 0,
)
