package ru.foodbox.delivery.modules.catalog.domain

import java.time.Instant
import java.util.UUID

data class CatalogCategory(
    val id: UUID,
    val name: String,
    val slug: String,
    val imageIds: List<UUID> = emptyList(),
    val imageUrls: List<String> = emptyList(),
    val isActive: Boolean,
    val createdAt: Instant,
    val updatedAt: Instant,
    val externalId: String? = null,
    val parentId: UUID? = null,
    val description: String? = null,
    val sortOrder: Int = 0,
)
