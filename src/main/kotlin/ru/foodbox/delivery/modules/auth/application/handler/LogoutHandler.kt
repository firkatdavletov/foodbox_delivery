package ru.foodbox.delivery.modules.auth.application.handler

import org.springframework.stereotype.Service
import ru.foodbox.delivery.modules.auth.api.request.LogoutRequest
import ru.foodbox.delivery.modules.auth.application.service.RefreshTokenService
import ru.foodbox.delivery.modules.auth.domain.repository.AuthSessionRepository
import java.time.Instant
import java.util.UUID

@Service
class LogoutHandler(
    private val authSessionRepository: AuthSessionRepository,
    private val refreshTokenService: RefreshTokenService
) {
    fun handle(userId: UUID, request: LogoutRequest) {
        val token = request.refreshToken ?: return
        val hash = refreshTokenService.hash(token)
        val session = authSessionRepository.findByRefreshTokenHash(hash) ?: return

        if (session.userId == userId) {
            authSessionRepository.revokeById(session.id, Instant.now())
        }
    }
}
