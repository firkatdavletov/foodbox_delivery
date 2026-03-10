package ru.foodbox.delivery.modules.auth.application.handler

import jakarta.servlet.http.HttpServletRequest
import org.springframework.stereotype.Service
import ru.foodbox.delivery.modules.auth.api.request.TelegramCompleteAuthRequest
import ru.foodbox.delivery.modules.auth.api.response.AuthTokensResponse
import ru.foodbox.delivery.modules.auth.application.service.AuthSessionIssuer
import ru.foodbox.delivery.modules.auth.domain.AuthIdentity
import ru.foodbox.delivery.modules.auth.domain.IdentityType
import ru.foodbox.delivery.modules.auth.domain.repository.AuthIdentityRepository
import ru.foodbox.delivery.modules.auth.infrastructure.provider.telegram.TelegramAuthProvider
import ru.foodbox.delivery.modules.user.domain.User
import ru.foodbox.delivery.modules.user.domain.repository.UserRepository
import java.time.Instant
import java.util.UUID

@Service
class CompleteTelegramAuthHandler(
    private val telegramAuthProvider: TelegramAuthProvider,
    private val authIdentityRepository: AuthIdentityRepository,
    private val userRepository: UserRepository,
    private val authSessionIssuer: AuthSessionIssuer
) {
    fun handle(request: TelegramCompleteAuthRequest, httpRequest: HttpServletRequest): AuthTokensResponse {
        val now = Instant.now()
        val payload = telegramAuthProvider.verify(request.authPayload)

        val existingIdentity = authIdentityRepository.findByTypeAndExternalId(
            IdentityType.TELEGRAM,
            payload.externalId
        )

        val isNewUser: Boolean
        val userId: UUID

        if (existingIdentity != null) {
            userId = existingIdentity.userId
            isNewUser = false
        } else {
            val newUser = User(
                id = UUID.randomUUID(),
                login = payload.login,
                name = payload.displayName,
            )
            val user = userRepository.create(newUser)
            authIdentityRepository.save(
                AuthIdentity(
                    id = UUID.randomUUID(),
                    userId = user.id,
                    type = IdentityType.TELEGRAM,
                    externalId = payload.externalId,
                    normalizedLogin = payload.login,
                    isVerified = true,
                    createdAt = now,
                    lastUsedAt = now
                )
            )
            userId = user.id
            isNewUser = true
        }

        val issued = authSessionIssuer.issue(
            userId = userId,
            deviceId = request.deviceId,
            userAgent = httpRequest.getHeader("User-Agent"),
            ip = httpRequest.remoteAddr
        )

        return AuthTokensResponse(
            accessToken = issued.accessToken,
            accessTokenExpiresAt = issued.accessTokenExpiresAt,
            refreshToken = issued.refreshToken,
            refreshTokenExpiresAt = issued.refreshTokenExpiresAt,
            isNewUser = isNewUser,
            userId = userId
        )
    }
}
