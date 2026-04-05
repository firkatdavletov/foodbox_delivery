package ru.foodbox.delivery.modules.orders.infrastructure.persistence.jpa

import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Query
import org.springframework.data.repository.query.Param
import ru.foodbox.delivery.modules.orders.infrastructure.persistence.entity.OrderStatusHistoryEntity
import java.util.UUID

interface OrderStatusHistoryJpaRepository : JpaRepository<OrderStatusHistoryEntity, UUID> {
    fun findAllByOrderIdOrderByChangedAtAsc(orderId: UUID): List<OrderStatusHistoryEntity>
    fun existsByOrderId(orderId: UUID): Boolean

    @Query(
        """
        select history
        from OrderStatusHistoryEntity history
        join fetch history.currentStatus
        where history.order.id in :orderIds
        order by history.order.id asc, history.changedAt asc
        """
    )
    fun findAllByOrderIdInWithCurrentStatus(
        @Param("orderIds") orderIds: Collection<UUID>,
    ): List<OrderStatusHistoryEntity>
}
