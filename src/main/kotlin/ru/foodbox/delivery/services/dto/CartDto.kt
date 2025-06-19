package ru.foodbox.delivery.services.dto

import ru.foodbox.delivery.data.DeliveryType
import ru.foodbox.delivery.services.dto.CartItemDto

data class CartDto(
    val items: List<CartItemDto>,
    val deliveryType: DeliveryType,
    val deliveryAddress: AddressDto?,
    val deliveryPrice: Double,
    val totalPrice: Double,
    val departmentId: Long?,
)