package ru.foodbox.delivery.modules.user.domain

import java.util.UUID

data class User(
    val id: UUID,
    val login: String? = null,
    val phone: String? = null,
    val name: String? = null,
    val email: String? = null,
    val company: String? = null,
)