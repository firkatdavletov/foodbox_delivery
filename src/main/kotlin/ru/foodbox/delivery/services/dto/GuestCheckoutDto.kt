package ru.foodbox.delivery.services.dto

import ru.foodbox.delivery.data.DeliveryType
import ru.foodbox.delivery.data.entities.OrderStatus
import java.time.LocalDateTime

data class GuestCheckoutItemInputDto(
    val productId: Long?,
    val sku: String?,
    val quantity: Int,
)

data class GuestCheckoutCustomerInputDto(
    val name: String,
    val phone: String,
    val email: String?,
)

data class GuestCheckoutDeliveryInputDto(
    val type: DeliveryType,
    val address: AddressDto?,
    val pickupPointId: Long?,
)

data class GuestCheckoutResultDto(
    val orderId: Long,
    val orderNumber: String?,
    val status: OrderStatus,
    val createdAt: LocalDateTime,
    val totalAmount: Long,
)
