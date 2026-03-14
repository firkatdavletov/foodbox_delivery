package ru.foodbox.delivery.modules.catalog.domain

import java.util.UUID

data class CatalogProductOptionGroup(
    val id: UUID,
    val productId: UUID,
    val code: String,
    val title: String,
    val sortOrder: Int = 0,
)
