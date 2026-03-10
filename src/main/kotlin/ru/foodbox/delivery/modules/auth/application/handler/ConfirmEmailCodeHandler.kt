package ru.foodbox.delivery.modules.auth.application.handler

import jakarta.servlet.http.HttpServletRequest
import org.springframework.stereotype.Service
import ru.foodbox.delivery.common.error.ForbiddenException
import ru.foodbox.delivery.common.error.NotFoundException
import ru.foodbox.delivery.modules.auth.api.request.ConfirmEmailCodeRequest
import ru.foodbox.delivery.modules.auth.api.response.AuthTokensResponse
import ru.foodbox.delivery.modules.auth.application.service.AuthSessionIssuer
import ru.foodbox.delivery.modules.auth.domain.AuthIdentity
import ru.foodbox.delivery.modules.auth.domain.AuthMethod
import ru.foodbox.delivery.modules.auth.domain.IdentityType
import ru.foodbox.delivery.modules.auth.domain.repository.AuthChallengeRepository
import ru.foodbox.delivery.modules.auth.domain.repository.AuthIdentityRepository
import ru.foodbox.delivery.modules.user.domain.User
import ru.foodbox.delivery.modules.user.domain.repository.UserRepository
import java.time.Instant
import java.util.UUID

@Service
class ConfirmEmailCodeHandler(
    private val authChallengeRepository: AuthChallengeRepository,
    private val authIdentityRepository: AuthIdentityRepository,
    private val userRepository: UserRepository,
    private val authSessionIssuer: AuthSessionIssuer,
) {

    fun handle(request: ConfirmEmailCodeRequest, httpRequest: HttpServletRequest): AuthTokensResponse {
        val now = Instant.now()
        val challenge = authChallengeRepository.findById(request.challengeId)
            ?: throw NotFoundException("Challenge not found")

        if (challenge.method != AuthMethod.EMAIL) {
            throw ForbiddenException("Challenge method is not email based")
        }

        challenge.ensurePending(now)

        if (challenge.codeHash != request.code) {
            challenge.failAttempt(now)
            authChallengeRepository.save(challenge)
            throw ForbiddenException("Invalid code")
        }

        challenge.verifyNow(now)
        authChallengeRepository.save(challenge)

        val email = challenge.target
        val existingIdentity = authIdentityRepository.findByTypeAndNormalizedLogin(IdentityType.EMAIL, email)

        val isNewUser: Boolean
        val userId: UUID

        if (existingIdentity != null) {
            userId = existingIdentity.userId
            isNewUser = false
        } else {
            val newUser = User(
                id = UUID.randomUUID(),
                email = email,
                login = email,
            )
            val user = userRepository.create(newUser)
            authIdentityRepository.save(
                AuthIdentity(
                    id = UUID.randomUUID(),
                    userId = user.id,
                    type = IdentityType.EMAIL,
                    externalId = email,
                    normalizedLogin = email,
                    isVerified = true,
                    createdAt = now,
                    lastUsedAt = now,
                )
            )
            userId = user.id
            isNewUser = true
        }

        val issued = authSessionIssuer.issue(
            userId = userId,
            deviceId = request.deviceId,
            userAgent = httpRequest.getHeader("User-Agent"),
            ip = httpRequest.remoteAddr,
        )

        return AuthTokensResponse(
            accessToken = issued.accessToken,
            accessTokenExpiresAt = issued.accessTokenExpiresAt,
            refreshToken = issued.refreshToken,
            refreshTokenExpiresAt = issued.refreshTokenExpiresAt,
            isNewUser = isNewUser,
            userId = userId,
        )
    }
}
