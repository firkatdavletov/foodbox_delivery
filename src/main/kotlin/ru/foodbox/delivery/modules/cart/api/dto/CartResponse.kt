package ru.foodbox.delivery.modules.cart.api.dto

import ru.foodbox.delivery.modules.cart.domain.CartStatus
import java.util.UUID

data class CartResponse(
    val id: UUID,
    val status: CartStatus,
    val items: List<CartItemResponse>,
    val totalPriceMinor: Long,
)
