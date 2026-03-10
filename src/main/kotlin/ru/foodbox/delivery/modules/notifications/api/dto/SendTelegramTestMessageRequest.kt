package ru.foodbox.delivery.modules.notifications.api.dto

import jakarta.validation.constraints.NotBlank

data class SendTelegramTestMessageRequest(
    @field:NotBlank
    val message: String,
    val chatIds: List<String>? = null,
)
