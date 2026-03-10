package ru.foodbox.delivery.modules.auth.api.request

import jakarta.validation.constraints.Email

data class StartEmailAuthRequest(
    @field:Email
    val email: String
)