package ru.foodbox.delivery.modules.catalog.domain

import java.time.Instant
import java.util.UUID

data class CatalogProductVariant(
    val id: UUID,
    val productId: UUID,
    val externalId: String? = null,
    val sku: String,
    val title: String? = null,
    val priceMinor: Long? = null,
    val oldPriceMinor: Long? = null,
    val imageUrls: List<String> = emptyList(),
    val sortOrder: Int = 0,
    val isActive: Boolean = true,
    val createdAt: Instant,
    val updatedAt: Instant,
)
