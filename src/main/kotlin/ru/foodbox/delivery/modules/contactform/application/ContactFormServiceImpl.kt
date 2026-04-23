package ru.foodbox.delivery.modules.contactform.application

import org.slf4j.LoggerFactory
import org.springframework.stereotype.Service
import ru.foodbox.delivery.modules.notifications.application.TelegramNotificationService

@Service
class ContactFormServiceImpl(
    private val telegramNotificationService: TelegramNotificationService,
) : ContactFormService {

    private val log = LoggerFactory.getLogger(javaClass)

    override fun submit(name: String, contact: String, question: String, comment: String?) {
        val message = buildMessage(name, contact, question, comment)
        val result = telegramNotificationService.sendToDefaultChats(message)
        if (result.failedRecipients > 0) {
            log.warn("Contact form notification: sent=${result.sentRecipients}, failed=${result.failedRecipients}")
        }
    }

    private fun buildMessage(name: String, contact: String, question: String, comment: String?): String {
        return buildString {
            appendLine("Новый вопрос с сайта")
            appendLine()
            appendLine("Имя: $name")
            appendLine("Контакт: $contact")
            appendLine("Вопрос: $question")
            if (!comment.isNullOrBlank()) {
                appendLine("Комментарий: $comment")
            }
        }.trimEnd()
    }
}
