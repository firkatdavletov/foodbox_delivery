package ru.foodbox.delivery.modules.auth.api.request

import jakarta.validation.constraints.NotBlank

data class TelegramCompleteAuthRequest(
    @field:NotBlank
    val authPayload: String,

    val deviceId: String?
)