package ru.foodbox.delivery.modules.orders.domain

import ru.foodbox.delivery.common.security.UserRole
import java.util.UUID

data class OrderStatusTransition(
    val id: UUID,
    val fromStatus: OrderStatusDefinition,
    val toStatus: OrderStatusDefinition,
    val requiredRole: UserRole?,
    val isAutomatic: Boolean,
    val guardCode: String?,
    val isActive: Boolean,
)
