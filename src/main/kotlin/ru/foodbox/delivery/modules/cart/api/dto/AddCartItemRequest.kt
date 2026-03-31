package ru.foodbox.delivery.modules.cart.api.dto

import jakarta.validation.constraints.Min
import jakarta.validation.constraints.NotNull
import jakarta.validation.Valid
import java.util.UUID

data class AddCartItemRequest(
    @field:NotNull
    val productId: UUID,

    val variantId: UUID? = null,

    @field:Min(1)
    val quantity: Int,

    @field:Valid
    val modifiers: List<AddCartItemModifierRequest> = emptyList(),
)

data class AddCartItemModifierRequest(
    @field:NotNull
    val modifierGroupId: UUID,

    @field:NotNull
    val modifierOptionId: UUID,

    @field:Min(1)
    val quantity: Int = 1,
)
