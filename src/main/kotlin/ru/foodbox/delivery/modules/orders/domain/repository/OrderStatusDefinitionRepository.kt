package ru.foodbox.delivery.modules.orders.domain.repository

import ru.foodbox.delivery.modules.orders.domain.OrderStateType
import ru.foodbox.delivery.modules.orders.domain.OrderStatusDefinition
import java.util.UUID

interface OrderStatusDefinitionRepository {
    fun findAll(includeInactive: Boolean = true): List<OrderStatusDefinition>
    fun findById(id: UUID): OrderStatusDefinition?
    fun findByCode(code: String): OrderStatusDefinition?
    fun findByStateType(stateType: OrderStateType): List<OrderStatusDefinition>
    fun findInitial(): OrderStatusDefinition?
    fun save(status: OrderStatusDefinition): OrderStatusDefinition
}
