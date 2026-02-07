package ru.foodbox.delivery.services.mapper

import org.springframework.stereotype.Component
import ru.foodbox.delivery.data.entities.OrderEntity
import ru.foodbox.delivery.data.entities.OrderItemEntity
import ru.foodbox.delivery.services.dto.OrderItemDto
import java.math.BigDecimal

@Component
class OrderItemMapper {
    fun toDto(entity: OrderItemEntity) = OrderItemDto(
        productId = entity.productId,
        price = entity.price
            .multiply(BigDecimal(100))
            .longValueExact(),
        name = entity.name,
        quantity = entity.quantity,
        imageUrl = entity.imageUrl,
        unit = entity.unit,
        totalPrice = entity.totalPrice
            .multiply(BigDecimal(100))
            .longValueExact(),
    )

    fun toDto(entities: List<OrderItemEntity>) = entities.map {
        toDto(it)
    }

    fun toEntity(dto: OrderItemDto, order: OrderEntity): OrderItemEntity = OrderItemEntity(
        order = order,
        productId = dto.productId,
        name = dto.name,
        quantity = dto.quantity,
        price = dto.price.toBigDecimal(),
        imageUrl = dto.imageUrl,
        unit = dto.unit,
        totalPrice = dto.totalPrice.toBigDecimal(),
    )

    fun toEntity(dtos: List<OrderItemDto>, order: OrderEntity): List<OrderItemEntity> = dtos.map {
        toEntity(it, order)
    }
}