package ru.foodbox.delivery.modules.orders.infrastructure.persistence.jpa

import org.springframework.data.jpa.repository.JpaRepository
import ru.foodbox.delivery.modules.orders.domain.OrderStateType
import ru.foodbox.delivery.modules.orders.infrastructure.persistence.entity.OrderStatusDefinitionEntity
import java.util.UUID

interface OrderStatusDefinitionJpaRepository : JpaRepository<OrderStatusDefinitionEntity, UUID> {
    fun findAllByOrderBySortOrderAscNameAsc(): List<OrderStatusDefinitionEntity>
    fun findAllByIsActiveTrueOrderBySortOrderAscNameAsc(): List<OrderStatusDefinitionEntity>
    fun findByCode(code: String): OrderStatusDefinitionEntity?
    fun findAllByStateTypeOrderBySortOrderAscNameAsc(stateType: OrderStateType): List<OrderStatusDefinitionEntity>
    fun findFirstByIsInitialTrueAndIsActiveTrue(): OrderStatusDefinitionEntity?
}
