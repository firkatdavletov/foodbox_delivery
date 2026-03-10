package ru.foodbox.delivery.modules.cart.domain

data class CartOwner(
    val type: CartOwnerType,
    val value: String,
)
