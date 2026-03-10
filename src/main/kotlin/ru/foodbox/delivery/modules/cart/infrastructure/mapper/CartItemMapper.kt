package ru.foodbox.delivery.modules.cart.infrastructure.mapper

import org.springframework.stereotype.Component
import ru.foodbox.delivery.modules.cart.infrastructure.persistance.entity.CartItemEntity
import ru.foodbox.delivery.modules.cart.domain.CartItem
import java.math.BigDecimal

@Component
class CartItemMapper {
    fun toDto(entity: CartItemEntity) = CartItem(
        productId = entity.product.id!!,
        title = entity.product.title,
        quantity = entity.quantity,
        price = entity.product.price,
        countStep = entity.product.countStep,
        unit = entity.product.unit
    )

    fun toDto(entities: List<CartItemEntity>) = entities
        .sortedBy { it.createdAt }
        .map { toDto(it) }
}