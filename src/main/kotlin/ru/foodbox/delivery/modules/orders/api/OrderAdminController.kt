package ru.foodbox.delivery.modules.orders.api

import jakarta.validation.Valid
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PatchMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RequestParam
import org.springframework.web.bind.annotation.RestController
import ru.foodbox.delivery.modules.orders.api.dto.OrderResponse
import ru.foodbox.delivery.modules.orders.api.dto.UpdateOrderStatusRequest
import ru.foodbox.delivery.modules.orders.application.OrderService
import java.util.UUID

@RestController
@RequestMapping("/api/v1/admin/orders")
class OrderAdminController(
    private val orderService: OrderService,
) {

    @GetMapping
    fun getOrders(): List<OrderResponse> {
        return orderService.getAdminOrders().map { it.toResponse() }
    }

    @GetMapping("/search")
    fun getOrderByNumber(
        @RequestParam(name = "orderNumber") orderNumber: String,
    ): OrderResponse {
        return orderService.getAdminOrderByNumber(orderNumber).toResponse()
    }

    @PatchMapping("/{orderId}/status")
    fun updateStatus(
        @PathVariable orderId: UUID,
        @Valid @RequestBody request: UpdateOrderStatusRequest,
    ): OrderResponse {
        return orderService.updateStatus(orderId, request.status).toResponse()
    }
}
