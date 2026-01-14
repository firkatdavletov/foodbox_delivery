package ru.foodbox.delivery.controllers.order

import org.springframework.http.HttpStatusCode
import org.springframework.http.ResponseEntity
import org.springframework.security.core.authority.SimpleGrantedAuthority
import org.springframework.security.core.context.SecurityContextHolder
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RequestParam
import org.springframework.web.bind.annotation.RestController
import org.springframework.web.server.ResponseStatusException
import ru.foodbox.delivery.controllers.order.body.CreateOrderRequestBody
import ru.foodbox.delivery.controllers.order.body.CreateOrderResponse
import ru.foodbox.delivery.services.OrderService
import ru.foodbox.delivery.controllers.order.body.GetCurrentOrdersResponse
import ru.foodbox.delivery.controllers.order.body.GetOrderRequestBody
import ru.foodbox.delivery.controllers.order.body.GetOrderResponse
import ru.foodbox.delivery.services.CartService
import kotlin.collections.contains

@RestController
@RequestMapping("/orders")
class OrderController(
    private val orderService: OrderService,
    private val cartService: CartService,
) {
    @GetMapping("/currentOrder")
    fun getOrder(@RequestParam("id") id: String): ResponseEntity<GetOrderResponse> {
        val authentication = SecurityContextHolder.getContext().authentication

        if (authentication.authorities.contains(SimpleGrantedAuthority("ROLE_ANONYMOUS"))) {
            throw ResponseStatusException(HttpStatusCode.valueOf(401), "Access denied")
        }
        val orderDto = orderService.getOrderById(id.toLong())
        return if (orderDto != null) {
            ResponseEntity.ok(GetOrderResponse(orderDto))
        } else {
            ResponseEntity.ok(GetOrderResponse("Get order error", 100))
        }
    }

    @GetMapping("/current")
    fun getCurrentOrders(): ResponseEntity<GetCurrentOrdersResponse> {
        val authentication = SecurityContextHolder.getContext().authentication

        if (authentication.authorities.contains(SimpleGrantedAuthority("ROLE_ANONYMOUS"))) {
            throw ResponseStatusException(HttpStatusCode.valueOf(401), "Access denied")
        }

        val userId = (SecurityContextHolder.getContext().authentication.principal as String).toLongOrNull()
            ?: throw ResponseStatusException(HttpStatusCode.valueOf(401), "Access denied")
        val createdOrder = orderService.getCurrentOrders(userId)
        val response = GetCurrentOrdersResponse(createdOrder)
        return ResponseEntity.ok(response)
    }

    @PostMapping("/createOrder")
    fun createOrder(@RequestBody request: CreateOrderRequestBody): ResponseEntity<CreateOrderResponse> {
        val authentication = SecurityContextHolder.getContext().authentication

        if (authentication.authorities.contains(SimpleGrantedAuthority("ROLE_ANONYMOUS"))) {
            throw ResponseStatusException(HttpStatusCode.valueOf(401), "Access denied")
        }

        val userId = (SecurityContextHolder.getContext().authentication.principal as String).toLongOrNull()
            ?: throw ResponseStatusException(HttpStatusCode.valueOf(401), "Access denied")

        val responseBody = orderService.createOrder(
            userId = userId,
            deliveryType = request.deliveryType,
            deliveryAddress = request.deliveryAddress,
            comment = request.comment,
            products = request.products,
            departmentId = request.departmentId,
            amount = request.amount,
            deliveryPrice = request.deliveryPrice,
        )
        return ResponseEntity.ok(responseBody)
    }
}