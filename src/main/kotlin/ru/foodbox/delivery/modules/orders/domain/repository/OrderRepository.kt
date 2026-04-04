package ru.foodbox.delivery.modules.orders.domain.repository

import ru.foodbox.delivery.modules.orders.domain.Order
import ru.foodbox.delivery.modules.orders.domain.OrderStateType
import java.util.UUID

interface OrderRepository {
    fun save(order: Order): Order
    fun findById(orderId: UUID): Order?
    fun findAllByCurrentStatusStateTypes(stateTypes: Set<OrderStateType>): List<Order>
    fun findByOrderNumber(orderNumber: String): Order?
    fun findByUserId(userId: UUID): List<Order>
    fun findByGuestInstallId(installId: String): List<Order>
    fun existsByCurrentStatusId(statusId: UUID): Boolean
}
