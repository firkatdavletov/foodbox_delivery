package ru.foodbox.delivery.controllers.cart.body

import ru.foodbox.delivery.data.DeliveryType
import ru.foodbox.delivery.services.dto.AddressDto
import ru.foodbox.delivery.services.model.DeliveryInfo

data class UpdateCartAddressRequestBody(
    val deliveryType: DeliveryType,
    val deliveryAddress: AddressDto?,
    val departmentId: Long,
    val deliveryInfo: DeliveryInfo,
    val comment: String?,
)