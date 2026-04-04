package ru.foodbox.delivery.modules.orders.domain

import java.util.UUID

data class OrderStatusDefinition(
    val id: UUID,
    val code: String,
    val name: String,
    val description: String?,
    val stateType: OrderStateType,
    val color: String?,
    val icon: String?,
    val isInitial: Boolean,
    val isFinal: Boolean,
    val isCancellable: Boolean,
    val isActive: Boolean,
    val visibleToCustomer: Boolean,
    val notifyCustomer: Boolean,
    val notifyStaff: Boolean,
    val sortOrder: Int,
)
