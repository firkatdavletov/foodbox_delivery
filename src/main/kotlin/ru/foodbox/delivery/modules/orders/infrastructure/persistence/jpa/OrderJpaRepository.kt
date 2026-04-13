package ru.foodbox.delivery.modules.orders.infrastructure.persistence.jpa

import org.springframework.data.domain.Page
import org.springframework.data.domain.Pageable
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.JpaSpecificationExecutor
import ru.foodbox.delivery.modules.orders.domain.OrderStateType
import ru.foodbox.delivery.modules.orders.infrastructure.persistence.entity.OrderEntity
import java.util.UUID

interface OrderJpaRepository : JpaRepository<OrderEntity, UUID>, JpaSpecificationExecutor<OrderEntity> {
    fun countByCurrentStatusStateTypeIn(stateTypes: Collection<OrderStateType>): Long
    fun findAllByCurrentStatusStateTypeInOrderByCreatedAtDesc(stateTypes: Collection<OrderStateType>): List<OrderEntity>
    fun findByOrderNumber(orderNumber: String): OrderEntity?
    fun findAllByUserIdOrderByCreatedAtDesc(userId: UUID): List<OrderEntity>
    fun findAllByGuestInstallIdOrderByCreatedAtDesc(guestInstallId: String): List<OrderEntity>
    fun existsByCurrentStatusId(statusId: UUID): Boolean
    override fun findAll(spec: org.springframework.data.jpa.domain.Specification<OrderEntity>?, pageable: Pageable): Page<OrderEntity>
}
