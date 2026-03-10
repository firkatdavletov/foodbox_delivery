package ru.foodbox.delivery.modules.cart.application.policy

import ru.foodbox.delivery.modules.cart.domain.Cart

interface CartMergePolicy {
    fun merge(source: Cart, target: Cart): Cart
}