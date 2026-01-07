package ru.foodbox.delivery.controllers.auth

import org.springframework.beans.factory.annotation.Value
import org.springframework.http.MediaType
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.*
import org.springframework.web.multipart.MultipartHttpServletRequest
import ru.foodbox.delivery.controllers.auth.body.*
import ru.foodbox.delivery.services.AuthService
import ru.foodbox.delivery.services.CartService
import java.security.MessageDigest

@RestController
@RequestMapping("/auth")
class AuthController(
    private val authService: AuthService,
    private val cartService: CartService,
    @param:Value("\${sms.ru.api.key}") private val apiKey: String
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

    @PostMapping(
        "/webhookClient",
        consumes = [MediaType.MULTIPART_FORM_DATA_VALUE],
        produces = [MediaType.TEXT_PLAIN_VALUE]
    )
    fun webhookClient(
        request: MultipartHttpServletRequest
    ): String {
        val params = request.parameterMap
        val data = params["data"] ?: params["data[]"] ?: emptyArray()
        val hash = params["hash"]?.firstOrNull() ?: return "406"

        if (data.isEmpty()) {
            return "100"
        }

        // === Проверка подписи ===
        val concatenatedData = buildString {
            data.forEach { append(it) }
        }

        val calculatedHash = sha256(apiKey + concatenatedData)

        if (!hash.equals(calculatedHash, ignoreCase = true)) {
            // Можно залогировать попытку
            return "407"
        }

        // === Обработка данных ===
        data.forEach { entry ->
            val lines = entry.split("\n")

            when (lines[0]) {
                "sms_status" -> {
                    // Здесь ваша бизнес-логика
                    // updateSmsStatus(smsId, smsStatus, unixTimestamp)
                }

                "callcheck_status" -> {
                    val checkId = lines.getOrNull(1)
                    val checkStatus = lines.getOrNull(2)

                    when (checkStatus) {
                        "401" -> {
                            authService.callCheckStatus(checkId)
                        }
                        "402" -> {
                            // Таймаут авторизации
                        }
                    }
                }
            }
        }

        return "100"
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

    private fun sha256(input: String): String {
        val digest = MessageDigest.getInstance("SHA-256")
        val hashBytes: ByteArray = digest.digest(input.toByteArray(Charsets.UTF_8))
        return hashBytes.joinToString("") { "%02x".format(it) }
    }
}