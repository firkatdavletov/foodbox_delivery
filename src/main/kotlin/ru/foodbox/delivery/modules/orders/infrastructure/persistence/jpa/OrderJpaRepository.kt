package ru.foodbox.delivery.modules.orders.infrastructure.persistence.jpa

import org.springframework.data.jpa.repository.JpaRepository
import ru.foodbox.delivery.modules.orders.domain.OrderStatus
import ru.foodbox.delivery.modules.orders.infrastructure.persistence.entity.OrderEntity
import java.util.UUID

interface OrderJpaRepository : JpaRepository<OrderEntity, UUID> {
    fun findAllByStatusInOrderByCreatedAtDesc(statuses: Collection<OrderStatus>): List<OrderEntity>
    fun findByOrderNumber(orderNumber: String): OrderEntity?
    fun findAllByUserIdOrderByCreatedAtDesc(userId: UUID): List<OrderEntity>
    fun findAllByGuestInstallIdOrderByCreatedAtDesc(guestInstallId: String): List<OrderEntity>
}
