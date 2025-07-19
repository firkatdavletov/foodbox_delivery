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
import org.springframework.web.bind.annotation.RequestParam
import org.springframework.web.bind.annotation.RestController
import org.springframework.web.server.ResponseStatusException
import ru.foodbox.delivery.controllers.payment.body.GetPaymentTypesResponse
import ru.foodbox.delivery.controllers.payment.body.PayOrderRequestBody
import ru.foodbox.delivery.data.entities.OrderStatus
import ru.foodbox.delivery.services.CartService
import ru.foodbox.delivery.services.OrderService
import ru.foodbox.delivery.services.PaymentService
import ru.foodbox.delivery.services.dto.PaymentDto

@RestController
@RequestMapping("/payment")
class PaymentController(
    private val paymentService: PaymentService,
    private val orderService: OrderService,
) {

    @GetMapping("/paymentTypes")
    fun getPaymentTypes(): ResponseEntity<GetPaymentTypesResponse> {
        val authentication = SecurityContextHolder.getContext().authentication
        if (authentication.authorities.contains(SimpleGrantedAuthority("ROLE_ANONYMOUS"))) {
            throw ResponseStatusException(HttpStatusCode.valueOf(401), "Access denied")
        }

        (SecurityContextHolder.getContext().authentication.principal as String).toLongOrNull()
            ?: throw ResponseStatusException(HttpStatusCode.valueOf(401), "Access denied")


        val paymentTypes = paymentService.getPaymentTypesByDepartmentId()
        val body = GetPaymentTypesResponse(paymentTypes)
        return ResponseEntity.ok(body)
    }

    @PostMapping("/payOrder")
    fun payOrder(
        @RequestBody body: PayOrderRequestBody,
        request: HttpServletRequest
    ): ResponseEntity<PaymentDto> {
        val authentication = SecurityContextHolder.getContext().authentication
        if (authentication.authorities.contains(SimpleGrantedAuthority("ROLE_ANONYMOUS"))) {
            throw ResponseStatusException(HttpStatusCode.valueOf(401), "Access denied")
        }

        val userId = (SecurityContextHolder.getContext().authentication.principal as String).toLongOrNull()
            ?: throw ResponseStatusException(HttpStatusCode.valueOf(401), "Access denied")

        val ipAddress = request.getHeader("X-Forwarded-For")

        val order = orderService.createOrder(userId)

        if (body.amount != order.totalAmount) return ResponseEntity.ok(
            PaymentDto(
                success = false,
                message = "Сумма отличается",
                paymentType = body.paymentType
            )
        )

        val paymentResponse = paymentService.pay(
            paymentType = body.paymentType,
            token = body.token,
            cryptogram = body.cryptogram,
            amount = body.amount,
            userId = userId,
            orderId = order.id,
            ipAddress = if (ipAddress.isNullOrBlank() || ipAddress == "unknown") request.remoteAddr else ipAddress
        )

        if (paymentResponse.success && paymentResponse.model != null) {
            val orderId = paymentResponse.model.orderId ?: throw ResponseStatusException(
                HttpStatusCode.valueOf(404),
                "Order not found"
            )
            if (paymentResponse.paymentType == "cash") {
                orderService.updateOrderStatus(orderId, OrderStatus.AWAITING_CASH_PAYMENT)
            } else {
                orderService.updateOrderStatus(orderId, OrderStatus.AWAITING_PAYMENT)
            }
        }

        return ResponseEntity.ok(paymentResponse)
    }
}