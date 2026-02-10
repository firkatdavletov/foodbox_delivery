package ru.foodbox.delivery.services.dto

import ru.foodbox.delivery.data.DeliveryType
import ru.foodbox.delivery.services.model.DeliveryInfo
import java.math.BigDecimal

data class CartDto(
    val items: List<CartItemDto>,
    val deliveryType: DeliveryType,
    val deliveryAddress: AddressDto?,
    val deliveryInfo: DeliveryInfo,
    val totalPrice: Long,
    val department: DepartmentDto,
    val comment: String?,
)