package ru.foodbox.delivery.modules.cart.api.dto

import jakarta.validation.constraints.Min
data class ChangeCartItemQuantityRequest(
    @field:Min(1)
    val quantity: Int,
)
