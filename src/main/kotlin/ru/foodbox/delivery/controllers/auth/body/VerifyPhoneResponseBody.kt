package ru.foodbox.delivery.controllers.auth.body

import ru.foodbox.delivery.services.dto.TokenPairDto

data class VerifyPhoneResponseBody(
    val tokens: TokenPairDto
)
