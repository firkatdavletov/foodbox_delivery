package ru.foodbox.delivery.data.entities

import java.math.BigDecimal
import java.time.LocalDateTime

class OrderPreviewEntity(
    val id: Long,
    val totalAmount: BigDecimal,
    val status: OrderStatus,
    val customerName: String,
    val companyName: String?,
    val deliveryTime: LocalDateTime,
)