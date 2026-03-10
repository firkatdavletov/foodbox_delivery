package ru.foodbox.delivery.modules.admin.auth.application.service

import jakarta.servlet.http.HttpServletRequest
import org.springframework.stereotype.Service
import ru.foodbox.delivery.common.error.ForbiddenException
import ru.foodbox.delivery.common.error.NotFoundException
import ru.foodbox.delivery.common.error.UnauthorizedException
import ru.foodbox.delivery.common.security.HashEncoder
import ru.foodbox.delivery.common.security.UserRole
import ru.foodbox.delivery.modules.admin.auth.api.request.AdminLoginRequest
import ru.foodbox.delivery.modules.admin.auth.api.request.AdminLogoutRequest
import ru.foodbox.delivery.modules.admin.auth.api.request.AdminRefreshTokenRequest
import ru.foodbox.delivery.modules.admin.auth.api.response.AdminAuthTokensResponse
import ru.foodbox.delivery.modules.admin.auth.domain.AdminAuthSession
import ru.foodbox.delivery.modules.admin.auth.domain.repository.AdminAuthSessionRepository
import ru.foodbox.delivery.modules.admin.auth.domain.repository.AdminUserRepository
import ru.foodbox.delivery.modules.auth.application.service.RefreshTokenService
import ru.foodbox.delivery.modules.auth.infrastructure.jwt.JwtAccessTokenService
import java.time.Instant
import java.time.temporal.ChronoUnit
import java.util.Locale
import java.util.UUID

@Service
class AdminAuthServiceImpl(
    private val adminUserRepository: AdminUserRepository,
    private val adminAuthSessionRepository: AdminAuthSessionRepository,
    private val hashEncoder: HashEncoder,
    private val refreshTokenService: RefreshTokenService,
    private val jwtAccessTokenService: JwtAccessTokenService,
) : AdminAuthService {

    override fun login(request: AdminLoginRequest, httpRequest: HttpServletRequest): AdminAuthTokensResponse {
        val normalizedLogin = normalizeLogin(request.login)
        val admin = adminUserRepository.findByNormalizedLogin(normalizedLogin)
            ?: throw UnauthorizedException("Invalid login or password")

        val isPasswordValid = hashEncoder.matches(request.password, admin.passwordHash)
        if (!isPasswordValid) {
            throw UnauthorizedException("Invalid login or password")
        }

        val issued = issueTokens(
            adminId = admin.id,
            deviceId = request.deviceId,
            httpRequest = httpRequest,
        )

        return issued.toResponse(admin.id)
    }

    override fun refresh(
        adminId: UUID,
        request: AdminRefreshTokenRequest,
        httpRequest: HttpServletRequest
    ): AdminAuthTokensResponse {
        val now = Instant.now()
        val oldHash = refreshTokenService.hash(request.refreshToken)
        val oldSession = adminAuthSessionRepository.findByRefreshTokenHash(oldHash)
            ?: throw NotFoundException("Refresh token not found")

        if (oldSession.adminId != adminId) {
            throw ForbiddenException("Refresh token does not belong to current admin")
        }

        if (!oldSession.isActive(now)) {
            throw ForbiddenException("Refresh token is expired or revoked")
        }

        adminAuthSessionRepository.revokeById(oldSession.id, now)

        val issued = issueTokens(
            adminId = oldSession.adminId,
            deviceId = request.deviceId ?: oldSession.deviceId,
            httpRequest = httpRequest,
        )

        return issued.toResponse(oldSession.adminId)
    }

    override fun logout(adminId: UUID, currentSessionId: UUID, request: AdminLogoutRequest?) {
        val refreshToken = request?.refreshToken?.trim().takeIf { !it.isNullOrBlank() }

        if (refreshToken != null) {
            val hash = refreshTokenService.hash(refreshToken)
            val session = adminAuthSessionRepository.findByRefreshTokenHash(hash) ?: return
            if (session.adminId != adminId) {
                throw ForbiddenException("Refresh token does not belong to current admin")
            }
            adminAuthSessionRepository.revokeById(session.id, Instant.now())
            return
        }

        adminAuthSessionRepository.revokeById(currentSessionId, Instant.now())
    }

    private fun issueTokens(
        adminId: UUID,
        deviceId: String?,
        httpRequest: HttpServletRequest
    ): IssuedAdminSessionTokens {
        val now = Instant.now()
        val sessionId = UUID.randomUUID()
        val accessTokenExpiresAt = now.plus(15, ChronoUnit.MINUTES)
        val refreshTokenExpiresAt = now.plus(30, ChronoUnit.DAYS)

        val rawRefreshToken = refreshTokenService.generateRawToken()
        val refreshTokenHash = refreshTokenService.hash(rawRefreshToken)

        adminAuthSessionRepository.save(
            AdminAuthSession(
                id = sessionId,
                adminId = adminId,
                deviceId = deviceId,
                userAgent = httpRequest.getHeader("User-Agent"),
                ip = httpRequest.remoteAddr,
                refreshTokenHash = refreshTokenHash,
                expiresAt = refreshTokenExpiresAt,
                revokedAt = null,
                createdAt = now,
                lastUsedAt = now,
            )
        )

        val accessToken = jwtAccessTokenService.generateAccessToken(
            userId = adminId,
            sessionId = sessionId,
            roles = listOf(UserRole.ADMIN),
            expiresAt = accessTokenExpiresAt,
        )

        return IssuedAdminSessionTokens(
            accessToken = accessToken,
            accessTokenExpiresAt = accessTokenExpiresAt,
            refreshToken = rawRefreshToken,
            refreshTokenExpiresAt = refreshTokenExpiresAt,
        )
    }

    private fun IssuedAdminSessionTokens.toResponse(adminId: UUID): AdminAuthTokensResponse =
        AdminAuthTokensResponse(
            accessToken = accessToken,
            accessTokenExpiresAt = accessTokenExpiresAt,
            refreshToken = refreshToken,
            refreshTokenExpiresAt = refreshTokenExpiresAt,
            adminId = adminId,
        )

    private fun normalizeLogin(login: String): String = login.trim().lowercase(Locale.ROOT)
}
