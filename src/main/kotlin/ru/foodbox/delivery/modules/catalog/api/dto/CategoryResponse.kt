package ru.foodbox.delivery.modules.catalog.api.dto

import java.util.UUID

data class CategoryResponse(
    val id: UUID,
    val name: String,
    val slug: String,
    val imageUrls: List<String>,
    val isActive: Boolean,
)

data class AdminCategoryResponse(
    val id: UUID,
    val name: String,
    val slug: String,
    val imageIds: List<UUID>,
    val imageUrls: List<String>,
    val isActive: Boolean,
)
