package ru.foodbox.delivery.modules.catalog.api.dto

import java.util.UUID

data class CategoryResponse(
    val id: UUID,
    val name: String,
    val slug: String,
    val imageUrls: List<String>,
    val isActive: Boolean,
)
