package ru.foodbox.delivery.modules.orders.domain.repository

import ru.foodbox.delivery.modules.orders.domain.OrderStatusTransition
import java.util.UUID

interface OrderStatusTransitionRepository {
    fun findAll(includeInactive: Boolean = true): List<OrderStatusTransition>
    fun findById(id: UUID): OrderStatusTransition?
    fun findAllByFromStatusId(fromStatusId: UUID, includeInactive: Boolean = false): List<OrderStatusTransition>
    fun findAllByStatusId(statusId: UUID, includeInactive: Boolean = true): List<OrderStatusTransition>
    fun findTransition(fromStatusId: UUID, toStatusId: UUID): OrderStatusTransition?
    fun save(transition: OrderStatusTransition): OrderStatusTransition
    fun deleteById(id: UUID)
}
