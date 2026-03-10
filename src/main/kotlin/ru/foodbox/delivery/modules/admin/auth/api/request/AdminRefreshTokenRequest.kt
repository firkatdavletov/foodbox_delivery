package ru.foodbox.delivery.modules.admin.auth.api.request

import jakarta.validation.constraints.NotBlank

data class AdminRefreshTokenRequest(
    @field:NotBlank
    val refreshToken: String,

    val deviceId: String? = null,
)
