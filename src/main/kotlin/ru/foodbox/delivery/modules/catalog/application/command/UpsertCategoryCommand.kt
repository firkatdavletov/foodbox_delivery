package ru.foodbox.delivery.modules.catalog.application.command

import java.util.UUID

data class UpsertCategoryCommand(
    val id: UUID?,
    val externalId: String?,
    val name: String,
    val slug: String?,
    val description: String?,
    val sortOrder: Int? = null,
    val imageIds: List<UUID> = emptyList(),
    val isActive: Boolean,
)
