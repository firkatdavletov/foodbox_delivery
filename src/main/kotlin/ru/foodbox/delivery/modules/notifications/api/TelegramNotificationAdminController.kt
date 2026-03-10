package ru.foodbox.delivery.modules.notifications.api

import jakarta.validation.Valid
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController
import ru.foodbox.delivery.modules.notifications.api.dto.SendTelegramTestMessageRequest
import ru.foodbox.delivery.modules.notifications.api.dto.SendTelegramTestMessageResponse
import ru.foodbox.delivery.modules.notifications.application.TelegramNotificationService

@RestController
@RequestMapping("/api/v1/admin/telegram")
class TelegramNotificationAdminController(
    private val telegramNotificationService: TelegramNotificationService,
) {

    // TEMPORARY endpoint for local verification of Telegram delivery.
    @PostMapping("/test-message")
    fun sendTestMessage(
        @Valid @RequestBody request: SendTelegramTestMessageRequest,
    ): SendTelegramTestMessageResponse {
        val result = telegramNotificationService.send(
            message = request.message,
            chatIds = request.chatIds,
        )

        return SendTelegramTestMessageResponse(
            attemptedRecipients = result.attemptedRecipients,
            sentRecipients = result.sentRecipients,
            failedRecipients = result.failedRecipients,
            skipped = result.skipped,
            skipReason = result.skipReason,
        )
    }
}
