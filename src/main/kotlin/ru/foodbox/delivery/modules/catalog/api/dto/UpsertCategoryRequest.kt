package ru.foodbox.delivery.modules.catalog.api.dto

import jakarta.validation.constraints.NotBlank
import java.util.UUID

data class UpsertCategoryRequest(
    val id: UUID? = null,

    @field:NotBlank
    val name: String,

    val slug: String? = null,
    val imageIds: List<UUID> = emptyList(),
    val isActive: Boolean = true,
)
