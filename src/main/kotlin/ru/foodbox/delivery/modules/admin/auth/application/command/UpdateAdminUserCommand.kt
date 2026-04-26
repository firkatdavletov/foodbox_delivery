package ru.foodbox.delivery.modules.admin.auth.application.command

import ru.foodbox.delivery.modules.admin.auth.domain.AdminRole

data class UpdateAdminUserCommand(
    val login: String,
    val role: AdminRole,
    val active: Boolean,
)
