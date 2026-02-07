package ru.foodbox.delivery.services.mapper

import org.springframework.stereotype.Component
import ru.foodbox.delivery.data.entities.OrderEntity
import ru.foodbox.delivery.services.dto.OrderDto
import java.math.BigDecimal

@Component
class OrderMapper(
    private val orderItemMapper: OrderItemMapper,
    private val userMapper: UserMapper,
) {
    fun toDto(entity: OrderEntity) = OrderDto(
        id = entity.id!!,
        user = userMapper.toDto(entity.user),
        status = entity.status,
        deliveryType = entity.deliveryType,
        deliveryAddress = entity.deliveryAddress,
        items = orderItemMapper.toDto(entity.items),
        deliveryPrice = entity.deliveryPrice
            .multiply(BigDecimal(100))
            .longValueExact(),
        deliveryTime = entity.deliveryTime,
        totalAmount = entity.totalAmount
            .multiply(BigDecimal(100))
            .longValueExact(),
        comment = entity.comment,
        created = entity.created,
        modified = entity.modified,
    )

    fun toDto(entities: List<OrderEntity>) = entities.map { toDto(it) }
}