package ru.foodbox.delivery.controllers.order.body

import ru.foodbox.delivery.data.DeliveryType
import ru.foodbox.delivery.services.dto.AddressDto
import ru.foodbox.delivery.services.dto.OrderItemDto
import java.math.BigDecimal

class CreateOrderRequestBody(
    val deliveryType: DeliveryType,
    val deliveryAddress: AddressDto?,
    val comment: String?,
    val products: List<OrderItemDto>,
    val departmentId: Long,
    val amount: Long,
    val deliveryPrice: Long,
)