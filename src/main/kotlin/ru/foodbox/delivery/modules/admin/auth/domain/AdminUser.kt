package ru.foodbox.delivery.modules.admin.auth.domain

import java.time.Instant
import java.util.UUID

data class AdminUser(
    val id: UUID,
    val login: String,
    val normalizedLogin: String,
    val passwordHash: String,
    val createdAt: Instant,
    val updatedAt: Instant,
)
