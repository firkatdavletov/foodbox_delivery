package ru.foodbox.delivery.modules.auth.application.handler

import org.springframework.stereotype.Service
import ru.foodbox.delivery.modules.auth.api.request.StartPhoneAuthRequest
import ru.foodbox.delivery.modules.auth.api.response.AuthChallengeResponse
import ru.foodbox.delivery.modules.auth.domain.AuthChallenge
import ru.foodbox.delivery.modules.auth.domain.AuthChallengeStatus
import ru.foodbox.delivery.modules.auth.domain.AuthMethod
import ru.foodbox.delivery.modules.auth.domain.repository.AuthChallengeRepository
import ru.foodbox.delivery.modules.auth.infrastructure.provider.CallAuthProvider
import ru.foodbox.delivery.modules.auth.infrastructure.provider.SmsSender
import java.time.Instant
import java.time.temporal.ChronoUnit
import java.util.*

@Service
class StartPhoneAuthHandler(
    private val authChallengeRepository: AuthChallengeRepository,
    private val smsSender: SmsSender,
    private val callAuthProvider: CallAuthProvider
) {
    fun handle(request: StartPhoneAuthRequest): AuthChallengeResponse {
        val normalizedPhone = normalizePhone(request.phone)
        val code = generateNumericCode()
        val now = Instant.now()

        val challenge = AuthChallenge(
            id = UUID.randomUUID(),
            method = request.method,
            target = normalizedPhone,
            status = AuthChallengeStatus.PENDING,
            codeHash = hashCode(code),
            externalState = null,
            expiresAt = now.plus(5, ChronoUnit.MINUTES),
            attemptsLeft = 5,
            createdAt = now,
            completedAt = null
        )

        authChallengeRepository.save(challenge)

        when (request.method) {
            AuthMethod.PHONE_SMS -> smsSender.sendCode(normalizedPhone, code)
            AuthMethod.PHONE_CALL -> callAuthProvider.start(normalizedPhone, code)
            else -> error("Unsupported method")
        }

        return AuthChallengeResponse(
            challengeId = challenge.id,
            method = challenge.method,
            expiresAt = challenge.expiresAt,
            resendAvailableAt = now.plus(30, ChronoUnit.SECONDS),
            nextStep = "CONFIRM_CODE"
        )
    }

    private fun normalizePhone(phone: String): String =
        phone.filter { it.isDigit() || it == '+' }

    private fun generateNumericCode(): String = "123456"
    private fun hashCode(code: String): String = code
}