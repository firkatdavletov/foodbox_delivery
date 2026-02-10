package ru.foodbox.delivery.controllers.order.body

import org.springframework.data.domain.Page
import ru.foodbox.delivery.services.dto.OrderPreviewDto

data class GetOrdersResponse(
    val orders: Page<OrderPreviewDto>,
)