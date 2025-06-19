package ru.foodbox.delivery.schedulers

import org.springframework.scheduling.annotation.Scheduled
import org.springframework.stereotype.Component
import ru.foodbox.delivery.services.ConfirmationCodeService

@Component
class ConfirmationCodeCleanUpScheduler(
    private val confirmationCodeService: ConfirmationCodeService
) {
    @Scheduled(fixedDelay = 3600000) // раз в час
    fun cleanupExpiredCodes() {
        confirmationCodeService.deleteExpiredCodes()
    }
}