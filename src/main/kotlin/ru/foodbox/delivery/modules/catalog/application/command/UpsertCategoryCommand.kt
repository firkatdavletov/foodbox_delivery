package ru.foodbox.delivery.modules.catalog.application.command

import java.util.UUID

data class UpsertCategoryCommand(
    val id: UUID?,
    val name: String,
    val slug: String?,
    val imageIds: List<UUID> = emptyList(),
    val isActive: Boolean,
)
