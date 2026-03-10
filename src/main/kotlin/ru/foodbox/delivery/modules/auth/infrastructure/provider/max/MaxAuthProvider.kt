package ru.foodbox.delivery.modules.auth.infrastructure.provider.max

interface MaxAuthProvider {
    fun verify(authPayload: String): ExternalIdentityPayload
}