package ru.foodbox.delivery.controllers.auth

import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController
import ru.foodbox.delivery.controllers.auth.body.AuthTypesResponseBody
import ru.foodbox.delivery.services.AuthService
import ru.foodbox.delivery.controllers.auth.body.SendSmsRequestBody
import ru.foodbox.delivery.controllers.auth.body.SendSmsResponseBody
import ru.foodbox.delivery.controllers.auth.body.RefreshTokenRequestBody
import ru.foodbox.delivery.controllers.auth.body.RefreshTokenResponseBody
import ru.foodbox.delivery.services.dto.TokenPairDto
import ru.foodbox.delivery.controllers.auth.body.VerifyPhoneRequestBody
import ru.foodbox.delivery.controllers.auth.body.VerifyPhoneResponseBody

@RestController
@RequestMapping("/auth")
class AuthController(
    private val authService: AuthService,
) {
    @PostMapping("/sendSms")
    fun sendSms(
        @RequestBody body: SendSmsRequestBody
    ): ResponseEntity<SendSmsResponseBody> {
        val status = authService.sendSms(body.phone)
        return when (status) {
            100 -> {
                val body = SendSmsResponseBody(
                    status = status,
                    success = true,
                    message = "Успешно"
                )
                ResponseEntity.ok(body)
            }
            else -> ResponseEntity.status(503).build()
        }
    }

    @PostMapping("/verifyPhone")
    fun verifyPhone(@RequestBody body: VerifyPhoneRequestBody): ResponseEntity<VerifyPhoneResponseBody> {
        val dto = authService.verifyPhone(body.phone, body.code)
        val body = VerifyPhoneResponseBody(dto)
        return ResponseEntity.ok(body)
    }

    @GetMapping("/authTypes")
    fun getAuthTypes(): ResponseEntity<AuthTypesResponseBody> {
        val dto =  authService.getAuthTypes()
        val body = AuthTypesResponseBody(dto.types)
        return ResponseEntity.ok(body)
    }

    @PostMapping("/refreshToken")
    fun refresh(@RequestBody body: RefreshTokenRequestBody): ResponseEntity<RefreshTokenResponseBody> {
        val dto = authService.refresh(body.refresh)
        val body = RefreshTokenResponseBody(dto)
        return ResponseEntity.ok(body)
    }
}