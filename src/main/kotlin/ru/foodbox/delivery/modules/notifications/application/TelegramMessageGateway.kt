package ru.foodbox.delivery.modules.notifications.application

data class TelegramSendOutcome(
    val delivered: Boolean,
    val reason: String? = null,
)

interface TelegramMessageGateway {
    fun sendMessage(chatId: String, text: String): TelegramSendOutcome
}
