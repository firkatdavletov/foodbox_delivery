package ru.foodbox.delivery.modules.orders.api.dto

import java.util.UUID

data class UpdateOrderStatusRequest(
    val statusId: UUID? = null,
    val statusCode: String? = null,
    val status: String? = null,
    val comment: String? = null,
)
