package ru.foodbox.delivery.modules.cart.api.dto

import jakarta.validation.constraints.NotBlank
import jakarta.validation.constraints.Size

data class ApplyCartPromoCodeRequest(
    @field:NotBlank
    @field:Size(max = 64)
    val code: String,
)
