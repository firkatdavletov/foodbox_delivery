package ru.foodbox.delivery.modules.auth.infrastructure.jwt

interface RefreshTokenService {
    fun generateRawToken(): String
    fun hash(rawToken: String): String
}