package ru.foodbox.delivery.modules.auth.api.request

import jakarta.validation.constraints.NotBlank
import jakarta.validation.constraints.NotNull
import java.util.UUID

data class ConfirmPhoneCodeRequest(
    @field:NotNull
    val challengeId: UUID,

    @field:NotBlank
    val code: String,

    val deviceId: String?
)