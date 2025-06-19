package ru.foodbox.delivery.controllers.cart.body

data class UpdateQuantityRequestBody(
    val productId: Long,
    val quantity: Int
)