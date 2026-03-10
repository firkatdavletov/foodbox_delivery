package ru.foodbox.delivery.modules.auth.api.response

import ru.foodbox.delivery.modules.auth.domain.AuthMethod
import java.time.Instant
import java.time.LocalDateTime
import java.util.UUID

data class AuthChallengeResponse(
    val challengeId: UUID,
    val method: AuthMethod,
    val expiresAt: Instant,
    val resendAvailableAt: Instant?,
    val nextStep: String
)