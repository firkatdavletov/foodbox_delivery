package ru.foodbox.delivery.services.dto

import ru.foodbox.delivery.data.DeliveryType
import ru.foodbox.delivery.services.model.DeliveryInfo

data class CartDto(
    val items: List<CartItemDto>,
    val deliveryType: DeliveryType,
    val deliveryAddress: AddressDto?,
    val deliveryInfo: DeliveryInfo,
    val totalPrice: Double,
    val department: DepartmentDto,
    val comment: String?,
)