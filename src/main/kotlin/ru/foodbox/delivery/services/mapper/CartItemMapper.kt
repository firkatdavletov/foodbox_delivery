package ru.foodbox.delivery.services.mapper

import org.springframework.stereotype.Component
import ru.foodbox.delivery.data.entities.CartItemEntity
import ru.foodbox.delivery.services.dto.CartItemDto

@Component
class CartItemMapper {
    fun toDto(entity: CartItemEntity) = CartItemDto(
        productId = entity.product.id,
        title = entity.product.title,
        quantity = entity.quantity,
        price = entity.product.price
    )

    fun toDto(entities: List<CartItemEntity>) = entities.map { toDto(it) }
}