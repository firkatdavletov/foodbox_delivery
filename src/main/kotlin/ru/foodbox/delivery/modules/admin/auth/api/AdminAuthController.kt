package ru.foodbox.delivery.modules.admin.auth.api

import jakarta.servlet.http.HttpServletRequest
import jakarta.validation.Valid
import org.springframework.http.ResponseEntity
import org.springframework.security.core.annotation.AuthenticationPrincipal
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController
import ru.foodbox.delivery.common.security.UserPrincipal
import ru.foodbox.delivery.modules.admin.auth.api.request.AdminLoginRequest
import ru.foodbox.delivery.modules.admin.auth.api.request.AdminLogoutRequest
import ru.foodbox.delivery.modules.admin.auth.api.request.AdminRefreshTokenRequest
import ru.foodbox.delivery.modules.admin.auth.api.response.AdminAuthTokensResponse
import ru.foodbox.delivery.modules.admin.auth.application.service.AdminAuthService

@RestController
@RequestMapping("/api/v1/admin")
class AdminAuthController(
    private val adminAuthService: AdminAuthService,
) {

    @PostMapping("/login")
    fun login(
        @Valid @RequestBody request: AdminLoginRequest,
        httpRequest: HttpServletRequest
    ): AdminAuthTokensResponse =
        adminAuthService.login(request, httpRequest)

    @PostMapping("/refresh")
    fun refresh(
        @AuthenticationPrincipal principal: UserPrincipal,
        @Valid @RequestBody request: AdminRefreshTokenRequest,
        httpRequest: HttpServletRequest
    ): AdminAuthTokensResponse =
        adminAuthService.refresh(principal.userId, request, httpRequest)

    @PostMapping("/logout")
    fun logout(
        @AuthenticationPrincipal principal: UserPrincipal,
        @RequestBody(required = false) request: AdminLogoutRequest?,
    ): ResponseEntity<Unit> {
        adminAuthService.logout(principal.userId, principal.sessionId, request)
        return ResponseEntity.noContent().build()
    }
}
