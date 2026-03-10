package ru.foodbox.delivery.modules.cart.application.command

import java.util.UUID

data class ChangeCartItemQuantityCommand(
    val productId: UUID,
    val quantity: Int,
)
