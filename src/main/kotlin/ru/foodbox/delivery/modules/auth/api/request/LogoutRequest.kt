package ru.foodbox.delivery.modules.auth.api.request

data class LogoutRequest(
    val refreshToken: String?
)