package ru.foodbox.delivery.modules.auth.application.service

import org.springframework.stereotype.Service
import ru.foodbox.delivery.common.security.UserRole
import ru.foodbox.delivery.modules.auth.domain.AuthSession
import ru.foodbox.delivery.modules.auth.domain.repository.AuthSessionRepository
import ru.foodbox.delivery.modules.auth.infrastructure.jwt.JwtAccessTokenService
import java.time.Instant
import java.time.temporal.ChronoUnit
import java.util.UUID

@Service
class AuthSessionIssuerImpl(
    private val authSessionRepository: AuthSessionRepository,
    private val jwtAccessTokenService: JwtAccessTokenService,
    private val refreshTokenService: RefreshTokenService,
) : AuthSessionIssuer {

    override fun issue(
        userId: UUID,
        deviceId: String?,
        userAgent: String?,
        ip: String?
    ): IssuedSessionTokens {
        val now = Instant.now()
        val sessionId = UUID.randomUUID()
        val accessTokenExpiresAt = now.plus(15, ChronoUnit.MINUTES)
        val refreshTokenExpiresAt = now.plus(30, ChronoUnit.DAYS)

        val rawRefreshToken = refreshTokenService.generateRawToken()
        val refreshTokenHash = refreshTokenService.hash(rawRefreshToken)

        val session = authSessionRepository.save(
            AuthSession(
                id = sessionId,
                userId = userId,
                deviceId = deviceId,
                userAgent = userAgent,
                ip = ip,
                refreshTokenHash = refreshTokenHash,
                expiresAt = refreshTokenExpiresAt,
                revokedAt = null,
                createdAt = now,
                lastUsedAt = now,
            )
        )

        val accessToken = jwtAccessTokenService.generateAccessToken(
            userId = userId,
            sessionId = session.id,
            roles = listOf(UserRole.CUSTOMER),
            expiresAt = accessTokenExpiresAt,
        )

        return IssuedSessionTokens(
            accessToken = accessToken,
            accessTokenExpiresAt = accessTokenExpiresAt,
            refreshToken = rawRefreshToken,
            refreshTokenExpiresAt = refreshTokenExpiresAt,
            session = session,
        )
    }
}
