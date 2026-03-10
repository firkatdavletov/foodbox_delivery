package ru.foodbox.delivery.modules.cart.domain.repository

import ru.foodbox.delivery.modules.cart.domain.Cart
import ru.foodbox.delivery.modules.cart.domain.CartOwner

interface CartRepository {
    fun findActiveByOwner(owner: CartOwner): Cart?
    fun save(cart: Cart): Cart
}