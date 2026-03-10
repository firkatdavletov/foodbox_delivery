package ru.foodbox.delivery.modules.cart.infrastructure.mapper

import org.springframework.stereotype.Component
import ru.foodbox.delivery.common.web.CurrentActor
import ru.foodbox.delivery.modules.cart.domain.Cart
import ru.foodbox.delivery.modules.cart.domain.CartOwner
import ru.foodbox.delivery.modules.cart.domain.CartOwnerType
import ru.foodbox.delivery.modules.cart.infrastructure.persistance.entity.CartEntity

@Component
class CartMapper(
    private val cartItemMapper: CartItemMapper,
) {
    fun toDto(
        entity: CartEntity,
    ) = Cart(
        id = entity.id,
        owner = CartOwner(
            value = entity.ownerId,
            type = entity.ownerType,
        ),
        status = entity.status,
        items = cartItemMapper.toDto(entity.items).toMutableList(),
        totalPrice = entity.totalPrice,
        createdAt = entity.createdAt,
        updatedAt = entity.updatedAt
    )

    fun toOwner(actor: CurrentActor): CartOwner =
        when (actor) {
            is CurrentActor.User -> CartOwner(CartOwnerType.USER, actor.userId.toString())
            is CurrentActor.Guest -> CartOwner(CartOwnerType.INSTALLATION, actor.installId)
        }
}