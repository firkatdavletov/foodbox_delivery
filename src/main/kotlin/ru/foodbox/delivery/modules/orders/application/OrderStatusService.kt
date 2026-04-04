package ru.foodbox.delivery.modules.orders.application

import ru.foodbox.delivery.modules.orders.application.command.ChangeOrderStatusCommand
import ru.foodbox.delivery.modules.orders.domain.Order
import ru.foodbox.delivery.modules.orders.domain.OrderStatusDefinition
import ru.foodbox.delivery.modules.orders.domain.OrderStatusHistory
import ru.foodbox.delivery.modules.orders.domain.OrderStatusTransition
import java.util.UUID

interface OrderStatusService {
    fun getInitialStatus(): OrderStatusDefinition
    fun recordInitialStatus(order: Order, actor: OrderStatusChangeActor, comment: String? = null)
    fun getAvailableTransitions(orderId: UUID, actor: OrderStatusChangeActor): List<OrderStatusTransition>
    fun changeStatus(orderId: UUID, command: ChangeOrderStatusCommand, actor: OrderStatusChangeActor): Order
    fun getStatusHistory(orderId: UUID): List<OrderStatusHistory>
}
