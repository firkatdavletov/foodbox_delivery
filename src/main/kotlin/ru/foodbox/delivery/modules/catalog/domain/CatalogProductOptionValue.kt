package ru.foodbox.delivery.modules.catalog.domain

import java.util.UUID

data class CatalogProductOptionValue(
    val id: UUID,
    val optionGroupId: UUID,
    val code: String,
    val title: String,
    val sortOrder: Int = 0,
)
