package ru.foodbox.delivery.services.dto

import ru.foodbox.delivery.data.entities.OrderStatus
import java.math.BigDecimal
import java.time.LocalDateTime

data class OrderPreviewDto(
    val id: Long,
    val totalAmount: BigDecimal,
    val status: OrderStatus,
    val customerName: String,
    val companyName: String?,
    val deliveryTime: LocalDateTime?,
)
