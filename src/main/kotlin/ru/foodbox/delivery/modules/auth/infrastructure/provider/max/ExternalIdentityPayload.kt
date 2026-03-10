package ru.foodbox.delivery.modules.auth.infrastructure.provider.max

data class ExternalIdentityPayload(
    val externalId: String,
    val login: String?,
    val displayName: String?
)