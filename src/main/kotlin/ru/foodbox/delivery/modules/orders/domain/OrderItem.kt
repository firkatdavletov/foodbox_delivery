package ru.foodbox.delivery.modules.orders.domain

import ru.foodbox.delivery.modules.catalog.domain.ProductUnit
import java.util.UUID

data class OrderItem(
    val id: UUID,
    val productId: UUID,
    val variantId: UUID?,
    val sku: String?,
    val title: String,
    val unit: ProductUnit,
    val quantity: Int,
    val priceMinor: Long,
    val totalMinor: Long,
)
