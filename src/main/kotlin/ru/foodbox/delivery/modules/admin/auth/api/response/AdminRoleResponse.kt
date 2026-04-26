package ru.foodbox.delivery.modules.admin.auth.api.response

import ru.foodbox.delivery.modules.admin.auth.domain.AdminRole

data class AdminRoleResponse(
    val code: AdminRole,
    val name: String,
)
