package ru.foodbox.delivery.modules.auth.domain.repository

import ru.foodbox.delivery.modules.auth.domain.AuthChallenge
import java.util.UUID

interface AuthChallengeRepository {
    fun save(challenge: AuthChallenge): AuthChallenge
    fun findById(id: UUID): AuthChallenge?
}