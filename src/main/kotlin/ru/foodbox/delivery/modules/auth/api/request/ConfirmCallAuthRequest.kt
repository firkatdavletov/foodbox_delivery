package ru.foodbox.delivery.modules.auth.api.request

import jakarta.validation.constraints.NotBlank
import jakarta.validation.constraints.NotNull

data class ConfirmCallAuthRequest(
    @field:NotNull
    val challengeId: Long,

    @field:NotBlank
    val verificationCode: String,

    val deviceId: String?
)