package ru.foodbox.delivery.modules.admin.auth.application.service

import jakarta.servlet.http.HttpServletRequest
import ru.foodbox.delivery.modules.admin.auth.api.request.AdminLoginRequest
import ru.foodbox.delivery.modules.admin.auth.api.request.AdminLogoutRequest
import ru.foodbox.delivery.modules.admin.auth.api.request.AdminRefreshTokenRequest
import ru.foodbox.delivery.modules.admin.auth.api.response.AdminAuthTokensResponse
import java.util.UUID

interface AdminAuthService {
    fun login(request: AdminLoginRequest, httpRequest: HttpServletRequest): AdminAuthTokensResponse
    fun refresh(
        adminId: UUID,
        request: AdminRefreshTokenRequest,
        httpRequest: HttpServletRequest
    ): AdminAuthTokensResponse

    fun logout(adminId: UUID, currentSessionId: UUID, request: AdminLogoutRequest?)
}
