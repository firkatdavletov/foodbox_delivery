package ru.foodbox.delivery.modules.auth.application.service

import java.util.UUID

interface AuthSessionIssuer {
    fun issue(
        userId: UUID,
        deviceId: String?,
        userAgent: String?,
        ip: String?
    ): IssuedSessionTokens
}