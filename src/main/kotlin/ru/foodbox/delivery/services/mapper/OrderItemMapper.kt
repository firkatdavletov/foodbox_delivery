package ru.foodbox.delivery.services.mapper

import org.springframework.stereotype.Component
import ru.foodbox.delivery.data.entities.OrderItemEntity
import ru.foodbox.delivery.services.dto.OrderItemDto

@Component
class OrderItemMapper {
    fun toDto(entity: OrderItemEntity) = OrderItemDto(
        productId = entity.productId,
        price = entity.price,
        name = entity.name,
        quantity = entity.quantity,
    )

    fun toDto(entities: List<OrderItemEntity>) = entities.map {
        toDto(it)
    }
}