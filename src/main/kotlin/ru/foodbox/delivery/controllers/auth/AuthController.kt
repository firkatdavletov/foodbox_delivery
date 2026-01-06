package ru.foodbox.delivery.controllers.auth

import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController
import ru.foodbox.delivery.controllers.auth.body.AuthByCallResponseModel
import ru.foodbox.delivery.controllers.auth.body.AuthTypesResponseBody
import ru.foodbox.delivery.controllers.auth.body.CartTokenResponseBody
import ru.foodbox.delivery.controllers.auth.body.CreateCartRequestBody
import ru.foodbox.delivery.services.AuthService
import ru.foodbox.delivery.controllers.auth.body.SendSmsRequestBody
import ru.foodbox.delivery.controllers.auth.body.SendSmsResponseBody
import ru.foodbox.delivery.controllers.auth.body.RefreshTokenRequestBody
import ru.foodbox.delivery.controllers.auth.body.RefreshTokenResponseBody
import ru.foodbox.delivery.controllers.auth.body.VerifyPhoneRequestBody
import ru.foodbox.delivery.controllers.auth.body.VerifyPhoneResponseBody
import ru.foodbox.delivery.data.sms_client.WebhookRequestBody
import ru.foodbox.delivery.services.CartService

@RestController
@RequestMapping("/auth")
class AuthController(
    private val authService: AuthService,
    private val cartService: CartService,
) {
    @PostMapping("/sendSms")
    fun sendSms(
        @RequestBody body: SendSmsRequestBody
    ): ResponseEntity<SendSmsResponseBody> {
        return when (val status = authService.sendSms(body.phone)) {
            100 -> {
                ResponseEntity.ok(SendSmsResponseBody(
                    status = status,
                ))
            }
            200 -> {
                ResponseEntity.ok(SendSmsResponseBody("Ошибка сервиса: невалидный токен", 200))
            }
            407 -> {
                ResponseEntity.ok(SendSmsResponseBody("Повторите позже", 407))
            }
            else -> ResponseEntity.ok(SendSmsResponseBody("Ошибка сервиса. Код ошибки: $status", status))
        }
    }
    @PostMapping("/byCall")
    fun authByCall(@RequestBody body: SendSmsRequestBody): AuthByCallResponseModel {
        val callCheck = authService.authByCall(body.phone)

        return if (callCheck != null) {
            AuthByCallResponseModel(callCheck)
        } else {
            AuthByCallResponseModel("Ошибка сервиса", 500)
        }
    }

    @PostMapping("/webhookSmsClient1003700")
    fun webhookSmsClient(@RequestBody body: WebhookRequestBody): Int {
        if (body.data.isEmpty()) return 100

        val type = body.data[0]

        when (type) {
            "callcheck_status" -> {
                if (body.data.size != 4) return 100

                val checkId = body.data[1]
                val status = body.data[2].toIntOrNull()
                val createdAt = body.data[3].toLongOrNull()
                authService.callCheckStatus(checkId, status, createdAt)
            }
        }

        return 100
    }

    @PostMapping("/verify")
    fun verifyPhone(@RequestBody body: VerifyPhoneRequestBody): ResponseEntity<VerifyPhoneResponseBody> {
        val dto = authService.verifyPhone(body.phone, body.code)

        return if (dto != null) {
            ResponseEntity.ok(VerifyPhoneResponseBody(dto))
        } else {
            ResponseEntity.ok(VerifyPhoneResponseBody("Error", 100))
        }
    }

    @GetMapping("/authTypes")
    fun getAuthTypes(): ResponseEntity<AuthTypesResponseBody> {
        val authTypes =  authService.getAuthTypes().types

        val body = if (authTypes.isNotEmpty()) {
            AuthTypesResponseBody(authTypes)
        } else {
            AuthTypesResponseBody("Нет доступных способов авторизации", 500)
        }

        return ResponseEntity.ok(body)
    }

    @PostMapping("/refreshTokens")
    fun refresh(@RequestBody body: RefreshTokenRequestBody): ResponseEntity<RefreshTokenResponseBody> {
        val dto = authService.refresh(body.refresh)

        return if (dto != null) {
            ResponseEntity.ok(RefreshTokenResponseBody(dto))
        } else {
            ResponseEntity.ok(RefreshTokenResponseBody("Not found", 100))
        }
    }

    @PostMapping("/createCart")
    fun createCart(@RequestBody body: CreateCartRequestBody): ResponseEntity<CartTokenResponseBody> {
        val token = cartService.createCart(
            deviceId = body.deviceId,
            departmentId = body.departmentId,
            deliveryAddress = body.deliveryAddress,
            deliveryType = body.deliveryType,
            deliveryPrice = body.deliveryPrice,
            freeDeliveryPrice = body.freeDeliveryPrice
        )
        return if (token != null) {
            ResponseEntity.ok(CartTokenResponseBody(token))
        } else {
            ResponseEntity.ok(CartTokenResponseBody(error = "Ошибка создания корзины", code = 404))
        }
    }
}