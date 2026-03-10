package ru.foodbox.delivery.modules.cart.api.response

data class CartItemResponse(
    val productId: Long,
    val quantity: Int,
    val priceSnapshot: Long
)