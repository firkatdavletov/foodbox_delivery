package ru.foodbox.delivery.controllers.web.checkout.body

import ru.foodbox.delivery.data.entities.OrderStatus
import ru.foodbox.delivery.services.dto.GuestCheckoutResultDto
import java.time.LocalDateTime

data class GuestCheckoutResponseBody(
    val orderId: Long,
    val orderNumber: String?,
    val status: OrderStatus,
    val createdAt: LocalDateTime,
    val totalAmount: Long,
) {
    constructor(dto: GuestCheckoutResultDto) : this(
        orderId = dto.orderId,
        orderNumber = dto.orderNumber,
        status = dto.status,
        createdAt = dto.createdAt,
        totalAmount = dto.totalAmount,
    )
}
