package ru.foodbox.delivery.modules.notifications.api.dto

data class SendTelegramTestMessageResponse(
    val attemptedRecipients: Int,
    val sentRecipients: Int,
    val failedRecipients: Int,
    val skipped: Boolean,
    val skipReason: String? = null,
)
