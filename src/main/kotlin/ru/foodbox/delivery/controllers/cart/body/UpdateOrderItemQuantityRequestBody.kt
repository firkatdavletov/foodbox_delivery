package ru.foodbox.delivery.controllers.cart.body

data class UpdateOrderItemQuantityRequestBody(
    val cartItemId: Long,
    val quantity: Int,
)
