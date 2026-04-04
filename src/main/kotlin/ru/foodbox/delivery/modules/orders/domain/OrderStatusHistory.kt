package ru.foodbox.delivery.modules.orders.domain

import java.time.Instant
import java.util.UUID

data class OrderStatusHistory(
    val id: UUID,
    val orderId: UUID,
    val previousStatus: OrderStatusDefinition?,
    val currentStatus: OrderStatusDefinition,
    val changeSourceType: OrderStatusChangeSourceType,
    val changedByUserId: UUID?,
    val comment: String?,
    val changedAt: Instant,
)
