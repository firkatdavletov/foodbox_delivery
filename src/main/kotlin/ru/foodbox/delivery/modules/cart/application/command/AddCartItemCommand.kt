package ru.foodbox.delivery.modules.cart.application.command

data class AddCartItemCommand(
    val productId: Long,
    val quantity: Int
)