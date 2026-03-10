package ru.foodbox.delivery.modules.cart.api.response

data class CartResponse(
    val items: List<CartItemResponse>,
    val totalPrice: Long,
)
