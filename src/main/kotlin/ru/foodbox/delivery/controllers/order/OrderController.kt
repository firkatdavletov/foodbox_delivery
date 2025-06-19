package ru.foodbox.delivery.controllers.order

import org.springframework.http.HttpStatusCode
import org.springframework.http.ResponseEntity
import org.springframework.security.core.authority.SimpleGrantedAuthority
import org.springframework.security.core.context.SecurityContextHolder
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController
import org.springframework.web.server.ResponseStatusException
import ru.foodbox.delivery.services.OrderService
import ru.foodbox.delivery.controllers.order.body.CreateOrderResponse
import kotlin.collections.contains

@RestController
@RequestMapping("/orders")
class OrderController(
    private val orderService: OrderService,
) {

    @GetMapping("/create")
    fun createOrder(): ResponseEntity<CreateOrderResponse> {
        val authentication = SecurityContextHolder.getContext().authentication

        if (authentication.authorities.contains(SimpleGrantedAuthority("ROLE_ANONYMOUS"))) {
            throw ResponseStatusException(HttpStatusCode.valueOf(401), "Access denied")
        }

        val userId = (SecurityContextHolder.getContext().authentication.principal as String).toLongOrNull()
            ?: throw ResponseStatusException(HttpStatusCode.valueOf(401), "Access denied")
        val createdOrder = orderService.createOrder(userId)
        val response = CreateOrderResponse(createdOrder)
        return ResponseEntity.ok(response)
    }
}