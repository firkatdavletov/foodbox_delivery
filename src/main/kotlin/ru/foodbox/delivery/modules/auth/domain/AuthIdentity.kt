package ru.foodbox.delivery.modules.auth.domain

import java.time.Instant
import java.time.LocalDateTime
import java.util.UUID

data class AuthIdentity(
    val id: UUID,
    val userId: UUID,
    val type: IdentityType,
    val externalId: String,
    val normalizedLogin: String?,
    val isVerified: Boolean,
    val createdAt: Instant,
    val lastUsedAt: Instant?
)