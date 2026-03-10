package ru.foodbox.delivery.modules.orders.domain.repository

import ru.foodbox.delivery.modules.orders.domain.Order
import java.util.UUID

interface OrderRepository {
    fun save(order: Order): Order
    fun findById(orderId: UUID): Order?
    fun findByUserId(userId: UUID): List<Order>
    fun findByGuestInstallId(installId: String): List<Order>
}
