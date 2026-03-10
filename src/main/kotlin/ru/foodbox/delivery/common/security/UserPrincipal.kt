package ru.foodbox.delivery.common.security

import java.util.UUID

data class UserPrincipal(
    val userId: UUID,
    val roles: Set<String>,
    val sessionId: UUID
)