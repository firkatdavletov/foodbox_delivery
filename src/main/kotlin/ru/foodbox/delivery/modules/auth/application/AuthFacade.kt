package ru.foodbox.delivery.modules.auth.application

import jakarta.servlet.http.HttpServletRequest
import ru.foodbox.delivery.modules.auth.api.request.ConfirmEmailCodeRequest
import ru.foodbox.delivery.modules.auth.api.request.ConfirmPhoneCodeRequest
import ru.foodbox.delivery.modules.auth.api.request.LogoutRequest
import ru.foodbox.delivery.modules.auth.api.request.MaxCompleteAuthRequest
import ru.foodbox.delivery.modules.auth.api.request.RefreshTokenRequest
import ru.foodbox.delivery.modules.auth.api.request.StartEmailAuthRequest
import ru.foodbox.delivery.modules.auth.api.request.StartPhoneAuthRequest
import ru.foodbox.delivery.modules.auth.api.request.TelegramCompleteAuthRequest
import ru.foodbox.delivery.modules.auth.api.response.AuthChallengeResponse
import ru.foodbox.delivery.modules.auth.api.response.AuthTokensResponse
import ru.foodbox.delivery.modules.auth.api.response.AvailableAuthMethodsResponse
import java.util.UUID

interface AuthFacade {
    fun getAvailableMethods(): AvailableAuthMethodsResponse

    fun startPhone(request: StartPhoneAuthRequest): AuthChallengeResponse
    fun confirmPhone(request: ConfirmPhoneCodeRequest, httpRequest: HttpServletRequest): AuthTokensResponse

    fun startEmail(request: StartEmailAuthRequest): AuthChallengeResponse
    fun confirmEmail(request: ConfirmEmailCodeRequest, httpRequest: HttpServletRequest): AuthTokensResponse

    fun completeTelegram(request: TelegramCompleteAuthRequest, httpRequest: HttpServletRequest): AuthTokensResponse
    fun completeMax(request: MaxCompleteAuthRequest, httpRequest: HttpServletRequest): AuthTokensResponse

    fun refresh(request: RefreshTokenRequest, httpRequest: HttpServletRequest): AuthTokensResponse

    fun logout(userId: UUID, request: LogoutRequest)
    fun logoutAll(userId: UUID)
}