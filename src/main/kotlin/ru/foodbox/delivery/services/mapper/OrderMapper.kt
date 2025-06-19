package ru.foodbox.delivery.services.mapper

import org.springframework.stereotype.Component
import ru.foodbox.delivery.data.entities.OrderEntity
import ru.foodbox.delivery.services.dto.OrderDto
import ru.foodbox.delivery.services.dto.OrderItemDto

@Component
class OrderMapper {
    fun toDto(entity: OrderEntity, items: List<OrderItemDto>) = OrderDto(
        id = entity.id,
        status = entity.status,
        items = items,
        deliveryPrice = entity.deliveryPrice,
        totalAmount = entity.totalAmount,
    )
}