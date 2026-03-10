package ru.foodbox.delivery.modules.auth.api.request

import jakarta.validation.constraints.NotBlank
import jakarta.validation.constraints.NotNull

data class ConfirmEmailCodeRequest(
    @field:NotNull
    val challengeId: Long,

    @field:NotBlank
    val code: String,

    val deviceId: String?
)