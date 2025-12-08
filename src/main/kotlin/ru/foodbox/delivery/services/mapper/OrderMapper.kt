package ru.foodbox.delivery.services.mapper

import org.springframework.stereotype.Component
import ru.foodbox.delivery.data.entities.OrderEntity
import ru.foodbox.delivery.data.entities.OrderItemEntity
import ru.foodbox.delivery.services.dto.OrderDto
import ru.foodbox.delivery.services.dto.OrderItemDto

@Component
class OrderMapper(
    private val orderItemMapper: OrderItemMapper,
) {
    fun toDto(entity: OrderEntity) = OrderDto(
        id = entity.id!!,
        status = entity.status,
        deliveryType = entity.deliveryType,
        deliveryAddress = entity.deliveryAddress,
        items = orderItemMapper.toDto(entity.items),
        deliveryPrice = entity.deliveryPrice,
        totalAmount = entity.totalAmount,
        comment = entity.comment
    )

    fun toDto(entities: List<OrderEntity>) = entities.map { toDto(it) }
}