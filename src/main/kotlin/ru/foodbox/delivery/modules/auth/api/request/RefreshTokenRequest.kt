package ru.foodbox.delivery.modules.auth.api.request

import jakarta.validation.constraints.NotBlank

data class RefreshTokenRequest(
    @field:NotBlank
    val refreshToken: String,

    val deviceId: String?
)