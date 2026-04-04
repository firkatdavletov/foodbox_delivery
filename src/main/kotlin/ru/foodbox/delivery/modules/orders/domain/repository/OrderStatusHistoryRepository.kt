package ru.foodbox.delivery.modules.orders.domain.repository

import ru.foodbox.delivery.modules.orders.domain.OrderStatusHistory
import java.util.UUID

interface OrderStatusHistoryRepository {
    fun save(history: OrderStatusHistory): OrderStatusHistory
    fun findAllByOrderId(orderId: UUID): List<OrderStatusHistory>
    fun existsByOrderId(orderId: UUID): Boolean
}
