package ru.foodbox.delivery.modules.orders.infrastructure.persistence.jpa

import org.springframework.data.jpa.repository.JpaRepository
import ru.foodbox.delivery.modules.orders.infrastructure.persistence.entity.OrderStatusTransitionEntity
import java.util.UUID

interface OrderStatusTransitionJpaRepository : JpaRepository<OrderStatusTransitionEntity, UUID> {
    fun findAllByOrderByIdAsc(): List<OrderStatusTransitionEntity>
    fun findAllByIsActiveTrueOrderByIdAsc(): List<OrderStatusTransitionEntity>
    fun findAllByFromStatusIdOrderByIdAsc(fromStatusId: UUID): List<OrderStatusTransitionEntity>
    fun findAllByFromStatusIdAndIsActiveTrueOrderByIdAsc(fromStatusId: UUID): List<OrderStatusTransitionEntity>
    fun findAllByFromStatusIdOrToStatusIdOrderByIdAsc(
        fromStatusId: UUID,
        toStatusId: UUID,
    ): List<OrderStatusTransitionEntity>
    fun findByFromStatusIdAndToStatusId(fromStatusId: UUID, toStatusId: UUID): OrderStatusTransitionEntity?
}
