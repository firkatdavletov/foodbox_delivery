package ru.foodbox.delivery.modules.notifications.application

import org.slf4j.LoggerFactory
import org.springframework.stereotype.Service
import ru.foodbox.delivery.modules.notifications.infrastructure.telegram.TelegramProperties

@Service
class TelegramNotificationServiceImpl(
    private val telegramProperties: TelegramProperties,
    private val telegramMessageGateway: TelegramMessageGateway,
) : TelegramNotificationService {

    private val logger = LoggerFactory.getLogger(TelegramNotificationServiceImpl::class.java)

    override fun sendToDefaultChats(message: String): NotificationDispatchResult {
        return send(
            message = message,
            chatIds = telegramProperties.defaultChatIds,
        )
    }

    override fun send(message: String, chatIds: List<String>?): NotificationDispatchResult {
        val normalizedMessage = message.trim()
        if (normalizedMessage.isBlank()) {
            return NotificationDispatchResult(
                attemptedRecipients = 0,
                sentRecipients = 0,
                failedRecipients = 0,
                skipped = true,
                skipReason = "Message is blank",
            )
        }

        if (!telegramProperties.enabled) {
            logger.debug("Telegram notifications are disabled; message skipped")
            return NotificationDispatchResult(
                attemptedRecipients = 0,
                sentRecipients = 0,
                failedRecipients = 0,
                skipped = true,
                skipReason = "Telegram notifications are disabled",
            )
        }

        if (telegramProperties.botToken.isBlank()) {
            logger.warn("Telegram notifications are enabled but bot token is blank; message skipped")
            return NotificationDispatchResult(
                attemptedRecipients = 0,
                sentRecipients = 0,
                failedRecipients = 0,
                skipped = true,
                skipReason = "Telegram bot token is blank",
            )
        }

        val recipients = normalizeChatIds(chatIds ?: telegramProperties.defaultChatIds)
        if (recipients.isEmpty()) {
            logger.warn("Telegram notifications are enabled but no chat ids are configured; message skipped")
            return NotificationDispatchResult(
                attemptedRecipients = 0,
                sentRecipients = 0,
                failedRecipients = 0,
                skipped = true,
                skipReason = "No Telegram chat ids configured",
            )
        }

        var sent = 0
        var failed = 0

        recipients.forEach { chatId ->
            val sendResult = telegramMessageGateway.sendMessage(chatId, normalizedMessage)
            if (sendResult.delivered) {
                sent += 1
            } else {
                failed += 1
                logger.warn(
                    "Telegram notification delivery failed for chatId={} reason={}",
                    chatId,
                    sendResult.reason ?: "unknown",
                )
            }
        }

        if (failed == 0) {
            logger.info("Telegram notification delivered to {}/{} recipients", sent, recipients.size)
        } else {
            logger.warn("Telegram notification delivered to {}/{} recipients", sent, recipients.size)
        }

        return NotificationDispatchResult(
            attemptedRecipients = recipients.size,
            sentRecipients = sent,
            failedRecipients = failed,
            skipped = false,
        )
    }

    private fun normalizeChatIds(chatIds: List<String>): List<String> {
        return chatIds.asSequence()
            .map { it.trim() }
            .filter { it.isNotBlank() }
            .distinct()
            .toList()
    }
}
