package ru.foodbox.delivery.modules.admin.auth.api

import ru.foodbox.delivery.modules.admin.auth.api.response.AdminRoleResponse
import ru.foodbox.delivery.modules.admin.auth.api.response.AdminUserResponse
import ru.foodbox.delivery.modules.admin.auth.domain.AdminRole
import ru.foodbox.delivery.modules.admin.auth.domain.AdminUser

fun AdminUser.toResponse(): AdminUserResponse =
    AdminUserResponse(
        id = id,
        login = login,
        role = role,
        active = active,
        createdAt = createdAt,
        updatedAt = updatedAt,
    )

fun AdminRole.toResponse(): AdminRoleResponse =
    AdminRoleResponse(
        code = this,
        name = when (this) {
            AdminRole.SUPERADMIN -> "Superadmin"
            AdminRole.OWNER -> "Owner"
            AdminRole.MANAGER -> "Manager"
            AdminRole.ORDER_MANAGER -> "Order manager"
            AdminRole.KITCHEN -> "Kitchen"
            AdminRole.DELIVERY_MANAGER -> "Delivery manager"
            AdminRole.CATALOG_MANAGER -> "Catalog manager"
            AdminRole.MARKETING_MANAGER -> "Marketing manager"
            AdminRole.SUPPORT -> "Support"
        },
    )
