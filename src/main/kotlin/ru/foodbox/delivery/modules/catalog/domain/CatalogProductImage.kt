package ru.foodbox.delivery.modules.catalog.domain

import java.time.Instant
import java.util.UUID

data class CatalogProductImage(
    val id: UUID,
    val productId: UUID,
    val imageId: UUID,
    val sortOrder: Int,
    val createdAt: Instant,
    val updatedAt: Instant,
)
