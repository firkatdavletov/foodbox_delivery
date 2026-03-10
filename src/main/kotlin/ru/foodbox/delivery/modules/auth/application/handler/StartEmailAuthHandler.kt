package ru.foodbox.delivery.modules.auth.application.handler

import org.springframework.stereotype.Service
import ru.foodbox.delivery.modules.auth.api.request.StartEmailAuthRequest
import ru.foodbox.delivery.modules.auth.api.response.AuthChallengeResponse
import ru.foodbox.delivery.modules.auth.domain.AuthChallenge
import ru.foodbox.delivery.modules.auth.domain.AuthChallengeStatus
import ru.foodbox.delivery.modules.auth.domain.AuthMethod
import ru.foodbox.delivery.modules.auth.domain.repository.AuthChallengeRepository
import ru.foodbox.delivery.modules.auth.infrastructure.provider.EmailAuthSender
import java.time.Instant
import java.time.temporal.ChronoUnit
import java.util.UUID

@Service
class StartEmailAuthHandler(
    private val authChallengeRepository: AuthChallengeRepository,
    private val emailAuthSender: EmailAuthSender,
) {

    fun handle(request: StartEmailAuthRequest): AuthChallengeResponse {
        val normalizedEmail = normalizeEmail(request.email)
        val code = generateNumericCode()
        val now = Instant.now()

        val challenge = AuthChallenge(
            id = UUID.randomUUID(),
            method = AuthMethod.EMAIL,
            target = normalizedEmail,
            status = AuthChallengeStatus.PENDING,
            codeHash = hashCode(code),
            externalState = null,
            expiresAt = now.plus(10, ChronoUnit.MINUTES),
            attemptsLeft = 5,
            createdAt = now,
            completedAt = null,
        )

        authChallengeRepository.save(challenge)
        emailAuthSender.sendCode(normalizedEmail, code)

        return AuthChallengeResponse(
            challengeId = challenge.id,
            method = challenge.method,
            expiresAt = challenge.expiresAt,
            resendAvailableAt = now.plus(30, ChronoUnit.SECONDS),
            nextStep = "CONFIRM_CODE",
        )
    }

    private fun normalizeEmail(email: String): String = email.trim().lowercase()

    private fun generateNumericCode(): String = "123456"

    private fun hashCode(code: String): String = code
}
