package ru.foodbox.delivery.services.dto

import ru.foodbox.delivery.data.DeliveryType
import ru.foodbox.delivery.data.entities.OrderCustomerType
import ru.foodbox.delivery.data.entities.OrderStatus
import ru.foodbox.delivery.modules.user.domain.User
import java.time.LocalDateTime

data class OrderDto(
    val id: Long,
    val user: User?,
    val customerType: OrderCustomerType,
    val customerName: String?,
    val customerPhone: String?,
    val customerEmail: String?,
    val status: OrderStatus,
    val deliveryType: DeliveryType,
    val deliveryAddress: String?,
    val deliveryTime: LocalDateTime?,
    val items: List<OrderItemDto>,
    val deliveryPrice: Long,
    val totalAmount: Long,
    val comment: String?,
    val created: LocalDateTime,
    val modified: LocalDateTime,
)
