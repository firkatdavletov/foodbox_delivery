package ru.foodbox.delivery.modules.auth.domain

import java.time.Instant
import java.util.UUID

data class AuthChallenge(
    val id: UUID,
    val method: AuthMethod,
    val target: String, // phone/email/external subject hint
    var status: AuthChallengeStatus,
    val codeHash: String?,
    val externalState: String?,
    val expiresAt: Instant,
    var attemptsLeft: Int,
    val createdAt: Instant,
    var completedAt: Instant?
) {
    fun verifyNow(now: Instant) {
        require(status == AuthChallengeStatus.PENDING) { "Challenge is not pending" }
        require(expiresAt.isAfter(now)) { "Challenge expired" }
        status = AuthChallengeStatus.VERIFIED
        completedAt = now
    }

    fun failAttempt(now: Instant) {
        require(status == AuthChallengeStatus.PENDING) { "Challenge is not pending" }
        attemptsLeft -= 1
        if (attemptsLeft <= 0 || !expiresAt.isAfter(now)) {
            status = AuthChallengeStatus.FAILED
        }
    }

    fun ensurePending(now: Instant) {
        require(status == AuthChallengeStatus.PENDING) { "Challenge is not pending" }
        require(expiresAt.isAfter(now)) { "Challenge expired" }
    }
}
