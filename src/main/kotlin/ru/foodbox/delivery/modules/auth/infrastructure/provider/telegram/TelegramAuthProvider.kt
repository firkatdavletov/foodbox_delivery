package ru.foodbox.delivery.modules.auth.infrastructure.provider.telegram

interface TelegramAuthProvider {
    fun verify(authPayload: String): ExternalIdentityPayload
}