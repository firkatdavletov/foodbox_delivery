package ru.foodbox.delivery.modules.admin.auth.api.request

data class AdminLogoutRequest(
    val refreshToken: String? = null,
)
