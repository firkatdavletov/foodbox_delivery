package ru.foodbox.delivery.modules.catalog.domain

import java.time.Instant
import java.util.UUID

data class CatalogCategory(
    val id: UUID,
    val name: String,
    val slug: String,
    val imageUrl: String?,
    val isActive: Boolean,
    val createdAt: Instant,
    val updatedAt: Instant,
)
