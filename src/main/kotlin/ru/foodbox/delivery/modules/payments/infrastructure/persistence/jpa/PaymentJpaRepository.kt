package ru.foodbox.delivery.modules.payments.infrastructure.persistence.jpa

import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Query
import org.springframework.data.repository.query.Param
import ru.foodbox.delivery.modules.orders.domain.OrderStateType
import ru.foodbox.delivery.modules.payments.domain.PaymentStatus
import ru.foodbox.delivery.modules.payments.infrastructure.persistence.entity.PaymentEntity
import java.time.Instant
import java.util.UUID

interface PaymentJpaRepository : JpaRepository<PaymentEntity, UUID> {
    @Query(
        """
        select count(distinct p.orderId)
        from PaymentEntity p
        where p.status = :status
          and p.paidAt >= :from
          and p.paidAt < :to
        """
    )
    fun countDistinctOrderIdsByStatusAndPaidAtBetween(
        @Param("status") status: PaymentStatus,
        @Param("from") from: Instant,
        @Param("to") to: Instant,
    ): Long

    @Query(
        """
        select count(distinct p.orderId)
        from PaymentEntity p
        where p.status in :statuses
          and p.orderId in (
              select o.id
              from OrderEntity o
              where o.currentStatus.stateType in :stateTypes
          )
          and p.createdAt = (
              select max(p2.createdAt)
              from PaymentEntity p2
              where p2.orderId = p.orderId
          )
        """
    )
    fun countDistinctOrderIdsByLatestStatusInAndOrderStateTypeIn(
        @Param("statuses") statuses: Collection<PaymentStatus>,
        @Param("stateTypes") stateTypes: Collection<OrderStateType>,
    ): Long

    fun findFirstByOrderIdAndStatusInOrderByCreatedAtDesc(
        orderId: UUID,
        statuses: Collection<PaymentStatus>,
    ): PaymentEntity?

    fun findFirstByOrderIdOrderByCreatedAtDesc(orderId: UUID): PaymentEntity?

    @Query(
        """
        select p
        from PaymentEntity p
        where p.orderId in :orderIds
          and p.createdAt = (
              select max(p2.createdAt)
              from PaymentEntity p2
              where p2.orderId = p.orderId
          )
        """
    )
    fun findLatestByOrderIdIn(
        @Param("orderIds") orderIds: Collection<UUID>,
    ): List<PaymentEntity>
}
