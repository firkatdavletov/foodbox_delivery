package ru.foodbox.delivery.controllers.auth.body

import ru.foodbox.delivery.services.dto.TokenPairDto

data class RefreshTokenResponseBody(
    val tokens: TokenPairDto
)