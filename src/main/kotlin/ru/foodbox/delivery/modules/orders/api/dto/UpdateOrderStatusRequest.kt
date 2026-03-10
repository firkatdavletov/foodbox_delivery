package ru.foodbox.delivery.modules.orders.api.dto

import jakarta.validation.constraints.NotNull
import ru.foodbox.delivery.modules.orders.domain.OrderStatus

data class UpdateOrderStatusRequest(
    @field:NotNull
    val status: OrderStatus,
)
