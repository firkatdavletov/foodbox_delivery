package ru.foodbox.delivery.modules.auth.api.request

import jakarta.validation.constraints.Email
import jakarta.validation.constraints.NotBlank

data class StartEmailAuthRequest(
    @field:NotBlank
    @field:Email
    val email: String
)
