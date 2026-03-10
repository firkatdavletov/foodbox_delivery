package ru.foodbox.delivery.modules.cart.api.dto

import jakarta.validation.constraints.Min
import jakarta.validation.constraints.NotNull
import java.util.UUID

data class AddCartItemRequest(
    @field:NotNull
    val productId: UUID,

    @field:Min(1)
    val quantity: Int,
)
