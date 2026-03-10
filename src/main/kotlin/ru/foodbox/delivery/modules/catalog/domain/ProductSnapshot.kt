package ru.foodbox.delivery.modules.catalog.domain

import java.util.UUID

data class ProductSnapshot(
    val id: UUID,
    val title: String,
    val unit: ProductUnit,
    val countStep: Int,
    val priceMinor: Long,
)
