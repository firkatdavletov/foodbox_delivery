package ru.foodbox.delivery.modules.catalog.domain

import java.time.Instant
import java.util.UUID

data class CatalogProductVariantImage(
    val id: UUID,
    val variantId: UUID,
    val imageId: UUID,
    val sortOrder: Int,
    val createdAt: Instant,
    val updatedAt: Instant,
)
