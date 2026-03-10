package ru.foodbox.delivery.modules.notifications.application

data class NotificationDispatchResult(
    val attemptedRecipients: Int,
    val sentRecipients: Int,
    val failedRecipients: Int,
    val skipped: Boolean,
    val skipReason: String? = null,
)

interface TelegramNotificationService {
    fun sendToDefaultChats(message: String): NotificationDispatchResult

    fun send(message: String, chatIds: List<String>? = null): NotificationDispatchResult
}
