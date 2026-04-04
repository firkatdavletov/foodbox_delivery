package ru.foodbox.delivery.modules.orders.application.command

import ru.foodbox.delivery.modules.orders.domain.OrderStateType
import java.util.UUID

data class ChangeOrderStatusCommand(
    val targetStatusId: UUID? = null,
    val targetStatusCode: String? = null,
    val targetStateType: OrderStateType? = null,
    val comment: String? = null,
)
