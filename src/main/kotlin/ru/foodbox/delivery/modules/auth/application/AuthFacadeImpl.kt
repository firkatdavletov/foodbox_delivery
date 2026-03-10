package ru.foodbox.delivery.modules.auth.application

import jakarta.servlet.http.HttpServletRequest
import org.springframework.stereotype.Service
import ru.foodbox.delivery.modules.auth.api.request.*
import ru.foodbox.delivery.modules.auth.api.response.AuthChallengeResponse
import ru.foodbox.delivery.modules.auth.api.response.AuthTokensResponse
import ru.foodbox.delivery.modules.auth.api.response.AvailableAuthMethodsResponse
import ru.foodbox.delivery.modules.auth.application.handler.*
import ru.foodbox.delivery.modules.auth.domain.AuthMethod
import java.util.*

@Service
class AuthFacadeImpl(
    private val startPhoneAuthHandler: StartPhoneAuthHandler,
    private val confirmPhoneCodeHandler: ConfirmPhoneCodeHandler,
    private val completeTelegramAuthHandler: CompleteTelegramAuthHandler,
    private val completeMaxAuthHandler: CompleteMaxAuthHandler,
    private val refreshSessionHandler: RefreshSessionHandler,
    private val logoutHandler: LogoutHandler,
    private val logoutAllHandler: LogoutAllHandler,
): AuthFacade {
    override fun getAvailableMethods(): AvailableAuthMethodsResponse {
        return AvailableAuthMethodsResponse(
            methods = setOf(
                AuthMethod.PHONE_SMS,
                AuthMethod.PHONE_CALL,
                AuthMethod.EMAIL,
                AuthMethod.TELEGRAM,
                AuthMethod.MAX
            )
        )
    }

    override fun startPhone(request: StartPhoneAuthRequest): AuthChallengeResponse {
        return startPhoneAuthHandler.handle(request)
    }

    override fun confirmPhone(
        request: ConfirmPhoneCodeRequest,
        httpRequest: HttpServletRequest
    ): AuthTokensResponse {
        return confirmPhoneCodeHandler.handle(request, httpRequest)
    }

    override fun startEmail(request: StartEmailAuthRequest): AuthChallengeResponse {
        TODO()
    }

    override fun confirmEmail(
        request: ConfirmEmailCodeRequest,
        httpRequest: HttpServletRequest
    ): AuthTokensResponse {
        TODO()
    }

    override fun completeTelegram(
        request: TelegramCompleteAuthRequest,
        httpRequest: HttpServletRequest
    ): AuthTokensResponse {
        return completeTelegramAuthHandler.handle(request, httpRequest)
    }

    override fun completeMax(
        request: MaxCompleteAuthRequest,
        httpRequest: HttpServletRequest
    ): AuthTokensResponse {
        return completeMaxAuthHandler.handle(request, httpRequest)
    }

    override fun refresh(
        request: RefreshTokenRequest,
        httpRequest: HttpServletRequest
    ): AuthTokensResponse {
        return refreshSessionHandler.handle(request, httpRequest)
    }

    override fun logout(userId: UUID, request: LogoutRequest) {
        logoutHandler.handle(userId, request)
    }

    override fun logoutAll(userId: UUID) {
        logoutAllHandler.handle(userId)
    }
}