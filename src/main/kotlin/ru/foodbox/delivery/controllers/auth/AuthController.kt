package ru.foodbox.delivery.controllers.auth

import org.slf4j.LoggerFactory
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
    private val log = LoggerFactory.getLogger(AuthController::class.java)
    @PostMapping("/verifyPhoneNumber")
    fun verifyPhoneNumber(
        @RequestBody body: VerifyPhoneNumberRequestBody
    ): ResponseEntity<VerifyPhoneNumberResponseBody> {
        val response = authService.verifyPhoneNumber(body.phone, body.type)
        return ResponseEntity.ok(response)
    }

    @PostMapping(
        "/webhookClient",
        consumes = [MediaType.MULTIPART_FORM_DATA_VALUE],
        produces = [MediaType.TEXT_PLAIN_VALUE]
    )
    fun webhookClient(
        request: MultipartHttpServletRequest
    ): String {
        log.info("Webhook received from IP: {}", request.remoteAddr)
        log.info("Content-Type: {}", request.contentType)
        val map: MutableMap<String, Array<String>> = HashMap()

        request.parameterMap.forEach { (key, values) ->
            log.info("Param: {} -> {}", key, values.joinToString())
            map[key] = values
            log.info("Key: $key")
            if (key.startsWith("data")) {
                val entries = values.getOrNull(0)?.split("\n")
                val type = entries?.getOrNull(0)
                log.info("Type: $type")

                when (type) {
                    "sms_status" -> {
                        // Здесь ваша бизнес-логика
                    }

                    "callcheck_status" -> {
                        val checkId = entries.getOrNull(1)
                        val checkStatus = entries.getOrNull(2)

                        log.info("CheckId: $checkId, CheckStatus: $checkStatus")

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
            } else if (key == "hash") {
                val hash = values.getOrNull(0)
                val concatenatedData = buildString {
                    map.values.forEach { append(it) }
                }
                val calculatedHash = sha256(concatenatedData)
                log.info("Received hash: {}", hash)
                log.info("Calculated hash: {}", calculatedHash)
            }
        }

        return "100"
    }

    @PostMapping("/checkSmsCode")
    fun checkSmsCode(@RequestBody body: VerifyPhoneRequestBody): ResponseEntity<TokenPairResponseBody> {
        val dto = authService.verifyPhone(body.phone, body.code)

        return if (dto != null) {
            ResponseEntity.ok(TokenPairResponseBody(dto))
        } else {
            ResponseEntity.ok(TokenPairResponseBody("Not authorized", 200))
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