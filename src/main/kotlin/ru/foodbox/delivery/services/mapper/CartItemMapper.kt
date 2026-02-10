package ru.foodbox.delivery.services.mapper

import org.springframework.stereotype.Component
import ru.foodbox.delivery.data.entities.CartItemEntity
import ru.foodbox.delivery.services.dto.CartItemDto
import java.math.BigDecimal

@Component
class CartItemMapper {
    fun toDto(entity: CartItemEntity) = CartItemDto(
        productId = entity.product.id!!,
        title = entity.product.title,
        quantity = entity.quantity,
        price = entity.product.price
            .multiply(BigDecimal(100))
            .longValueExact(),
        countStep = entity.product.countStep,
        unit = entity.product.unit
    )

    fun toDto(entities: List<CartItemEntity>) = entities
        .sortedBy { it.created }
        .map { toDto(it) }
}