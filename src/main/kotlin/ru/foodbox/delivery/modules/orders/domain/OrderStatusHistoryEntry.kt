package ru.foodbox.delivery.modules.orders.domain

import java.time.Instant

data class OrderStatusHistoryEntry(
    val code: String,
    val name: String,
    val timestamp: Instant,
)
