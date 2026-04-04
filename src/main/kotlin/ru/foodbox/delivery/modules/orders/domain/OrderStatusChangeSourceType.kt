package ru.foodbox.delivery.modules.orders.domain

enum class OrderStatusChangeSourceType {
    SYSTEM,
    ADMIN,
    CUSTOMER,
}
