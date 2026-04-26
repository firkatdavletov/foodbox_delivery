package ru.foodbox.delivery.modules.admin.auth.application.command

import ru.foodbox.delivery.modules.admin.auth.domain.AdminRole

data class CreateAdminUserCommand(
    val login: String,
    val password: String,
    val role: AdminRole,
    val active: Boolean,
)
