package ru.foodbox.delivery.modules.admin.auth.application

import ru.foodbox.delivery.modules.admin.auth.domain.AdminRole
import java.util.UUID

data class AdminUserManagementActor(
    val adminId: UUID,
    val roles: Set<String>,
) {
    fun hasRole(role: AdminRole): Boolean = role.name in roles
}
