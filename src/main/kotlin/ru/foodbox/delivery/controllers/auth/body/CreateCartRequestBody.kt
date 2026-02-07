package ru.foodbox.delivery.controllers.auth.body

import ru.foodbox.delivery.data.DeliveryType
import ru.foodbox.delivery.services.dto.AddressDto
import java.math.BigDecimal

data class CreateCartRequestBody(
    val deviceId: String,
    val deliveryType: DeliveryType,
    val deliveryAddress: AddressDto?,
    val departmentId: Int,
    val deliveryPrice: BigDecimal,
    val freeDeliveryPrice: BigDecimal?
)