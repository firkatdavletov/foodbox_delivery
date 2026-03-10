package ru.foodbox.delivery.modules.cart.application.command

data class ChangeCartItemQuantityCommand(
    val productId: Long,
    val quantity: Int
)