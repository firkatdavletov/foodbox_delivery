package ru.foodbox.delivery.modules.auth.application.service

interface RefreshTokenService {
    fun hash(rawToken: String): String
    fun generateRawToken(): String
}