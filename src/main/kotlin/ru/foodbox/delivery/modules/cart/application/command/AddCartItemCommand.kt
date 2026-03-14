package ru.foodbox.delivery.modules.cart.application.command

import java.util.UUID

data class AddCartItemCommand(
    val productId: UUID,
    val variantId: UUID?,
    val quantity: Int,
)
