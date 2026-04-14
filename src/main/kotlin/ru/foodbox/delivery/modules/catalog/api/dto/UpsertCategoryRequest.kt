package ru.foodbox.delivery.modules.catalog.api.dto

import jakarta.validation.constraints.NotBlank
import java.util.UUID

data class UpsertCategoryRequest(
    val id: UUID? = null,
    val externalId: String? = null,

    @field:NotBlank
    val name: String,

    val slug: String? = null,
    val description: String? = null,
    val sortOrder: Int? = null,
    val imageIds: List<UUID> = emptyList(),
    val isActive: Boolean = true,
)
