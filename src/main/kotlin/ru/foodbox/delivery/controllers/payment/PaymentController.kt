package ru.foodbox.delivery.controllers.payment

import jakarta.servlet.http.HttpServletRequest
import org.springframework.http.HttpStatusCode
import org.springframework.http.ResponseEntity
import org.springframework.security.core.authority.SimpleGrantedAuthority
import org.springframework.security.core.context.SecurityContextHolder
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController
import org.springframework.web.server.ResponseStatusException
import ru.foodbox.delivery.controllers.payment.body.GetPaymentTypesResponse
import ru.foodbox.delivery.controllers.payment.body.PayOrderRequestBody
import ru.foodbox.delivery.services.CartService
import ru.foodbox.delivery.services.OrderService
import ru.foodbox.delivery.services.PaymentService
import ru.foodbox.delivery.services.dto.PaymentDto

@RestController
@RequestMapping("/payment")
class PaymentController(
    private val paymentService: PaymentService,
    private val cartService: CartService,
    private val orderService: OrderService,
) {
    @GetMapping("/paymentTypes")
    fun getPaymentTypes(): ResponseEntity<GetPaymentTypesResponse> {
        val authentication = SecurityContextHolder.getContext().authentication
        if (authentication.authorities.contains(SimpleGrantedAuthority("ROLE_ANONYMOUS"))) {
            throw ResponseStatusException(HttpStatusCode.valueOf(401), "Access denied")
        }

        val userId = (SecurityContextHolder.getContext().authentication.principal as String).toLongOrNull()
            ?: throw ResponseStatusException(HttpStatusCode.valueOf(401), "Access denied")

        val cart = cartService.getCart(userId)
        val departmentId = cart.departmentId
            ?: throw ResponseStatusException(HttpStatusCode.valueOf(404), "Department id is null")

        val paymentTypes = paymentService.getPaymentTypesByDepartmentId(departmentId)
        val body = GetPaymentTypesResponse(paymentTypes)
        return ResponseEntity.ok(body)
    }

    @PostMapping("/payOrder")
    fun payOrder(
        @RequestBody body: PayOrderRequestBody,
    ): ResponseEntity<PaymentDto> {
        val authentication = SecurityContextHolder.getContext().authentication
        if (authentication.authorities.contains(SimpleGrantedAuthority("ROLE_ANONYMOUS"))) {
            throw ResponseStatusException(HttpStatusCode.valueOf(401), "Access denied")
        }

        val userId = (SecurityContextHolder.getContext().authentication.principal as String).toLongOrNull()
            ?: throw ResponseStatusException(HttpStatusCode.valueOf(401), "Access denied")

        val ipAddress = "192.168.24.67"//request.getHeader("X-Forwarded-For")

        val order = orderService.createOrder(userId)

        val paymentResponse = paymentService.pay(
            paymentType = body.paymentType,
            token = body.token,
            cryptogram = body.cryptogram,
            amount = order.totalAmount,
            userId = userId,
            orderId = order.id,
            ipAddress = ipAddress//if (ipAddress.isNullOrBlank() || ipAddress == "unknown") request.remoteAddr else ipAddress
        )
        return ResponseEntity.ok(paymentResponse)
    }
}