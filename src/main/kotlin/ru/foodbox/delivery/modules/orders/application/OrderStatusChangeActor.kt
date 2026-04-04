package ru.foodbox.delivery.modules.orders.application

import ru.foodbox.delivery.common.security.UserRole
import ru.foodbox.delivery.modules.orders.domain.OrderStatusChangeSourceType
import java.util.UUID

data class OrderStatusChangeActor(
    val sourceType: OrderStatusChangeSourceType,
    val actorId: UUID? = null,
    val roles: Set<UserRole> = emptySet(),
) {
    companion object {
        fun system(): OrderStatusChangeActor = OrderStatusChangeActor(
            sourceType = OrderStatusChangeSourceType.SYSTEM,
        )
    }
}
