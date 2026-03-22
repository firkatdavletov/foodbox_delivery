package ru.foodbox.delivery.modules.catalog.domain

import java.time.Instant
import java.util.UUID

data class CatalogCategoryImage(
    val id: UUID,
    val categoryId: UUID,
    val imageId: UUID,
    val sortOrder: Int,
    val createdAt: Instant,
    val updatedAt: Instant,
)
