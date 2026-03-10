package ru.foodbox.delivery.modules.auth.application.handler

import org.springframework.stereotype.Service
import ru.foodbox.delivery.modules.auth.domain.repository.AuthSessionRepository
import java.time.Instant
import java.util.UUID

@Service
class LogoutAllHandler(
    private val authSessionRepository: AuthSessionRepository
) {
    fun handle(userId: UUID) {
        authSessionRepository.revokeAllByUserId(userId, Instant.now())
    }
}
