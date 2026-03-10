package ru.foodbox.delivery.modules.auth.application.handler

import jakarta.servlet.http.HttpServletRequest
import org.springframework.stereotype.Service
import ru.foodbox.delivery.common.error.ForbiddenException
import ru.foodbox.delivery.common.error.NotFoundException
import ru.foodbox.delivery.modules.auth.api.request.RefreshTokenRequest
import ru.foodbox.delivery.modules.auth.api.response.AuthTokensResponse
import ru.foodbox.delivery.modules.auth.application.service.AuthSessionIssuer
import ru.foodbox.delivery.modules.auth.application.service.RefreshTokenService
import ru.foodbox.delivery.modules.auth.domain.repository.AuthSessionRepository
import java.time.Instant

@Service
class RefreshSessionHandler(
    private val authSessionRepository: AuthSessionRepository,
    private val refreshTokenService: RefreshTokenService,
    private val authSessionIssuer: AuthSessionIssuer
) {
    fun handle(request: RefreshTokenRequest, httpRequest: HttpServletRequest): AuthTokensResponse {
        val now = Instant.now()
        val oldHash = refreshTokenService.hash(request.refreshToken)
        val oldSession = authSessionRepository.findByRefreshTokenHash(oldHash)
            ?: throw NotFoundException("Refresh token not found")

        if (!oldSession.isActive(now)) {
            // Здесь можно добавить reuse detection и revoke всей цепочки
            throw ForbiddenException("Refresh token is expired or revoked")
        }

        authSessionRepository.revokeById(oldSession.id, now)

        val issued = authSessionIssuer.issue(
            userId = oldSession.userId,
            deviceId = request.deviceId ?: oldSession.deviceId,
            userAgent = httpRequest.getHeader("User-Agent"),
            ip = httpRequest.remoteAddr
        )

        return AuthTokensResponse(
            accessToken = issued.accessToken,
            accessTokenExpiresAt = issued.accessTokenExpiresAt,
            refreshToken = issued.refreshToken,
            refreshTokenExpiresAt = issued.refreshTokenExpiresAt,
            isNewUser = false,
            userId = oldSession.userId
        )
    }
}