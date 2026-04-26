package ru.foodbox.delivery.modules.admin.auth.api.response

import ru.foodbox.delivery.modules.admin.auth.domain.AdminRole
import java.time.Instant
import java.util.UUID

data class AdminUserResponse(
    val id: UUID,
    val login: String,
    val role: AdminRole,
    val active: Boolean,
    val createdAt: Instant,
    val updatedAt: Instant,
)
