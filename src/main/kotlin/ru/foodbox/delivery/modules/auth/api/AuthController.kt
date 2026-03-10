package ru.foodbox.delivery.modules.auth.api

import jakarta.servlet.http.HttpServletRequest
import jakarta.validation.Valid
import org.springframework.http.ResponseEntity
import org.springframework.security.core.annotation.AuthenticationPrincipal
import org.springframework.web.bind.annotation.*
import ru.foodbox.delivery.common.security.UserPrincipal
import ru.foodbox.delivery.modules.auth.api.request.*
import ru.foodbox.delivery.modules.auth.api.response.AuthChallengeResponse
import ru.foodbox.delivery.modules.auth.api.response.AuthTokensResponse
import ru.foodbox.delivery.modules.auth.api.response.AvailableAuthMethodsResponse
import ru.foodbox.delivery.modules.auth.application.AuthFacade

@RestController
@RequestMapping("/api/v1/auth")
class AuthController(
    private val authFacade: AuthFacade,
) {

    @GetMapping("/methods")
    fun methods(): AvailableAuthMethodsResponse {
        return authFacade.getAvailableMethods()
    }

    @PostMapping("/phone/start")
    fun phoneStart(@RequestBody request: StartPhoneAuthRequest): AuthChallengeResponse {
        return authFacade.startPhone(request)
    }

    @PostMapping("/phone/confirm")
    fun phoneConfirm(
        @Valid @RequestBody request: ConfirmPhoneCodeRequest,
        httpRequest: HttpServletRequest
    ): AuthTokensResponse {
        return authFacade.confirmPhone(request, httpRequest)
    }

    @PostMapping("/email/start")
    fun emailStart(@Valid @RequestBody request: StartEmailAuthRequest): AuthChallengeResponse {
        return authFacade.startEmail(request)
    }

    @PostMapping("/email/confirm")
    fun emailConfirm(
        @Valid @RequestBody request: ConfirmEmailCodeRequest,
        httpRequest: HttpServletRequest
    ): AuthTokensResponse {
        return authFacade.confirmEmail(request, httpRequest)
    }

    @PostMapping("/telegram/complete")
    fun completeTelegram(
        @Valid @RequestBody request: TelegramCompleteAuthRequest,
        httpRequest: HttpServletRequest
    ): AuthTokensResponse =
        authFacade.completeTelegram(request, httpRequest)

    @PostMapping("/max/complete")
    fun completeMax(
        @Valid @RequestBody request: MaxCompleteAuthRequest,
        httpRequest: HttpServletRequest
    ): AuthTokensResponse =
        authFacade.completeMax(request, httpRequest)

    @PostMapping("/refresh")
    fun refresh(
        @Valid @RequestBody request: RefreshTokenRequest,
        httpRequest: HttpServletRequest
    ): AuthTokensResponse =
        authFacade.refresh(request, httpRequest)

    @PostMapping("/logout")
    fun logout(
        @Valid @RequestBody request: LogoutRequest,
        @AuthenticationPrincipal principal: UserPrincipal
    ): ResponseEntity<Unit> {
        authFacade.logout(principal.userId, request)
        return ResponseEntity.noContent().build()
    }

    @PostMapping("/logout-all")
    fun logoutAll(
        @AuthenticationPrincipal principal: UserPrincipal
    ): ResponseEntity<Unit> {
        authFacade.logoutAll(principal.userId)
        return ResponseEntity.noContent().build()
    }
}
