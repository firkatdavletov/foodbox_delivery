package ru.foodbox.delivery.modules.orders.application.command

data class CheckoutCommand(
    val customerName: String?,
    val customerPhone: String?,
    val customerEmail: String?,
    val comment: String?,
)
