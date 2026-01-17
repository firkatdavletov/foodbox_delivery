package ru.foodbox.delivery.controllers.order.body

import ru.foodbox.delivery.controllers.base.ResponseModel
import ru.foodbox.delivery.services.dto.OrderDto

data class GetOrderResponse(
    val order: OrderDto?,
    override val success: Boolean,
    override val error: String?,
    override val code: Int?,
) : ResponseModel {
    constructor(orderDto: OrderDto) : this(orderDto, true, null, null)
    constructor(error: String, code: Int) : this(null, false, error, code)
}
