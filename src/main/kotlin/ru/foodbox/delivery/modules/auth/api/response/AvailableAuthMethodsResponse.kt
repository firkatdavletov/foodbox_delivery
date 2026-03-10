package ru.foodbox.delivery.modules.auth.api.response

import ru.foodbox.delivery.modules.auth.domain.AuthMethod

data class AvailableAuthMethodsResponse(
    val methods: Set<AuthMethod>
)