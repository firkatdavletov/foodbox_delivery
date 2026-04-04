package ru.foodbox.delivery.modules.orders.infrastructure.persistence.jpa

import org.springframework.data.jpa.repository.JpaRepository
import ru.foodbox.delivery.modules.orders.infrastructure.persistence.entity.OrderStatusHistoryEntity
import java.util.UUID

interface OrderStatusHistoryJpaRepository : JpaRepository<OrderStatusHistoryEntity, UUID> {
    fun findAllByOrderIdOrderByChangedAtAsc(orderId: UUID): List<OrderStatusHistoryEntity>
    fun existsByOrderId(orderId: UUID): Boolean
}
