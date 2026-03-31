package ru.foodbox.delivery.modules.cart.application.command

import java.util.UUID

data class AddCartItemCommand(
    val productId: UUID,
    val variantId: UUID?,
    val quantity: Int,
    val modifiers: List<AddCartItemModifierCommand> = emptyList(),
)

data class AddCartItemModifierCommand(
    val modifierGroupId: UUID,
    val modifierOptionId: UUID,
    val quantity: Int = 1,
)
