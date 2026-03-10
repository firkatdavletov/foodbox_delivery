package ru.foodbox.delivery.controllers.order.body

import ru.foodbox.delivery.common.utils.ResponseModel
import ru.foodbox.delivery.services.dto.OrderDto

class CreateOrderResponse(
    val order: OrderDto?,
    override val success: Boolean,
    override val error: String?,
    override val code: Int?
) : ResponseModel {
    constructor(order: OrderDto) : this(order, true, null, null)
    constructor(error: String, code: Int) : this(null, false, error, code)
}