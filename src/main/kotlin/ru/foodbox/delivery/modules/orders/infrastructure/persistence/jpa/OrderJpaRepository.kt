package ru.foodbox.delivery.modules.orders.infrastructure.persistence.jpa

import org.springframework.data.jpa.repository.JpaRepository
import ru.foodbox.delivery.modules.orders.infrastructure.persistence.entity.OrderEntity
import java.util.UUID

interface OrderJpaRepository : JpaRepository<OrderEntity, UUID> {
    fun findAllByUserIdOrderByCreatedAtDesc(userId: UUID): List<OrderEntity>
    fun findAllByGuestInstallIdOrderByCreatedAtDesc(guestInstallId: String): List<OrderEntity>
}
