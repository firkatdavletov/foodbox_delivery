package ru.foodbox.delivery.modules.orders.infrastructure.repository

import org.springframework.stereotype.Repository
import ru.foodbox.delivery.modules.orders.domain.OrderStatusHistory
import ru.foodbox.delivery.modules.orders.domain.repository.OrderStatusHistoryRepository
import ru.foodbox.delivery.modules.orders.infrastructure.persistence.entity.OrderStatusHistoryEntity
import ru.foodbox.delivery.modules.orders.infrastructure.persistence.jpa.OrderJpaRepository
import ru.foodbox.delivery.modules.orders.infrastructure.persistence.jpa.OrderStatusDefinitionJpaRepository
import ru.foodbox.delivery.modules.orders.infrastructure.persistence.jpa.OrderStatusHistoryJpaRepository

@Repository
class OrderStatusHistoryRepositoryImpl(
    private val jpaRepository: OrderStatusHistoryJpaRepository,
    private val orderJpaRepository: OrderJpaRepository,
    private val orderStatusDefinitionJpaRepository: OrderStatusDefinitionJpaRepository,
) : OrderStatusHistoryRepository {

    override fun save(history: OrderStatusHistory): OrderStatusHistory {
        return jpaRepository.save(
            OrderStatusHistoryEntity(
                id = history.id,
                order = orderJpaRepository.getReferenceById(history.orderId),
                previousStatus = history.previousStatus?.let { orderStatusDefinitionJpaRepository.getReferenceById(it.id) },
                currentStatus = orderStatusDefinitionJpaRepository.getReferenceById(history.currentStatus.id),
                changeSourceType = history.changeSourceType,
                changedByUserId = history.changedByUserId,
                comment = history.comment,
                changedAt = history.changedAt,
            )
        ).toDomain()
    }

    override fun findAllByOrderId(orderId: java.util.UUID): List<OrderStatusHistory> {
        return jpaRepository.findAllByOrderIdOrderByChangedAtAsc(orderId).map(OrderStatusHistoryEntity::toDomain)
    }

    override fun existsByOrderId(orderId: java.util.UUID): Boolean {
        return jpaRepository.existsByOrderId(orderId)
    }
}

internal fun OrderStatusHistoryEntity.toDomain(): OrderStatusHistory {
    return OrderStatusHistory(
        id = id,
        orderId = order.id,
        previousStatus = previousStatus?.toDomain(),
        currentStatus = currentStatus.toDomain(),
        changeSourceType = changeSourceType,
        changedByUserId = changedByUserId,
        comment = comment,
        changedAt = changedAt,
    )
}
