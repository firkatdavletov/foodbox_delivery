package ru.foodbox.delivery.modules.admin.auth.api.request

import jakarta.validation.constraints.NotBlank

data class AdminLoginRequest(
    @field:NotBlank
    val login: String,

    @field:NotBlank
    val password: String,

    val deviceId: String? = null,
)
