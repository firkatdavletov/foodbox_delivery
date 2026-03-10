package ru.foodbox.delivery.modules.cart.api.request

import jakarta.validation.constraints.Min
import jakarta.validation.constraints.NotNull

data class ChangeCartItemQuantityRequest(
    @field:NotNull
    val productId: Long,

    @field:Min(1)
    val quantity: Int
)