package ru.foodbox.delivery.modules.cart.domain.repository

import ru.foodbox.delivery.modules.cart.domain.Cart
import ru.foodbox.delivery.modules.cart.domain.CartOwner
import java.util.UUID

interface CartRepository {
    fun findById(cartId: UUID): Cart?
    fun findActiveByOwner(owner: CartOwner): Cart?
    fun save(cart: Cart): Cart
}
