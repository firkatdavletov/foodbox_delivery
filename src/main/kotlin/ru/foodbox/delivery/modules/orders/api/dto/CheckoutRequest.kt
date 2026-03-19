package ru.foodbox.delivery.modules.orders.api.dto

data class CheckoutRequest(
    val customerName: String? = null,
    val customerPhone: String? = null,
    val customerEmail: String? = null,
    val comment: String? = null,
)
