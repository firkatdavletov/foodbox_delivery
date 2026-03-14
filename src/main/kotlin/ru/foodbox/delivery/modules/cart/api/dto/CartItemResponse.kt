package ru.foodbox.delivery.modules.cart.api.dto

import ru.foodbox.delivery.modules.catalog.domain.ProductUnit
import java.util.UUID

data class CartItemResponse(
    val productId: UUID,
    val variantId: UUID?,
    val title: String,
    val unit: ProductUnit,
    val countStep: Int,
    val quantity: Int,
    val priceMinor: Long,
    val lineTotalMinor: Long,
)
