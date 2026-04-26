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
import ru.foodbox.delivery.modules.admin.auth.domain.AdminUser
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
        if (!admin.canAuthenticate()) {
            throw UnauthorizedException("Invalid login or password")
        }

        val isPasswordValid = hashEncoder.matches(request.password, admin.passwordHash)
        if (!isPasswordValid) {
            throw UnauthorizedException("Invalid login or password")
        }

        val issued = issueTokens(
            admin = admin,
            deviceId = request.deviceId,
            httpRequest = httpRequest,
        )

        return issued.toResponse(admin)
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

        val admin = adminUserRepository.findById(oldSession.adminId)
            ?: throw ForbiddenException("Admin user is not available")
        if (!admin.canAuthenticate()) {
            adminAuthSessionRepository.revokeById(oldSession.id, now)
            throw ForbiddenException("Admin user is not active")
        }

        adminAuthSessionRepository.revokeById(oldSession.id, now)

        val issued = issueTokens(
            admin = admin,
            deviceId = request.deviceId ?: oldSession.deviceId,
            httpRequest = httpRequest,
        )

        return issued.toResponse(admin)
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
        admin: AdminUser,
        deviceId: String?,
        httpRequest: HttpServletRequest
    ): IssuedAdminSessionTokens {
        val now = Instant.now()
        val sessionId = UUID.randomUUID()
        val accessTokenExpiresAt = now.plus(1, ChronoUnit.DAYS)
        val refreshTokenExpiresAt = now.plus(30, ChronoUnit.DAYS)

        val rawRefreshToken = refreshTokenService.generateRawToken()
        val refreshTokenHash = refreshTokenService.hash(rawRefreshToken)

        adminAuthSessionRepository.save(
            AdminAuthSession(
                id = sessionId,
                adminId = admin.id,
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
            userId = admin.id,
            sessionId = sessionId,
            roles = listOf(UserRole.ADMIN.name, admin.role.name),
            expiresAt = accessTokenExpiresAt,
        )

        return IssuedAdminSessionTokens(
            accessToken = accessToken,
            accessTokenExpiresAt = accessTokenExpiresAt,
            refreshToken = rawRefreshToken,
            refreshTokenExpiresAt = refreshTokenExpiresAt,
        )
    }

    private fun IssuedAdminSessionTokens.toResponse(admin: AdminUser): AdminAuthTokensResponse =
        AdminAuthTokensResponse(
            accessToken = accessToken,
            accessTokenExpiresAt = accessTokenExpiresAt,
            refreshToken = refreshToken,
            refreshTokenExpiresAt = refreshTokenExpiresAt,
            adminId = admin.id,
            role = admin.role,
        )

    private fun normalizeLogin(login: String): String = login.trim().lowercase(Locale.ROOT)
}
