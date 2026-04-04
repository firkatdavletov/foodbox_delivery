package ru.foodbox.delivery.modules.orders.domain

enum class OrderStateType {
    CREATED,
    AWAITING_CONFIRMATION,
    CONFIRMED,
    PREPARING,
    READY_FOR_PICKUP,
    OUT_FOR_DELIVERY,
    COMPLETED,
    CANCELED,
    ON_HOLD,
}
