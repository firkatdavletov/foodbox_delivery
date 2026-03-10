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
import ru.foodbox.delivery.controllers.payment.body.GetBanksResponse
import ru.foodbox.delivery.controllers.payment.body.GetPaymentTypesResponse
import ru.foodbox.delivery.controllers.payment.body.PayOrderRequestBody
import ru.foodbox.delivery.data.telegram.MessageService
import ru.foodbox.delivery.services.BankService
import ru.foodbox.delivery.services.OrderService
import ru.foodbox.delivery.services.PaymentService
import ru.foodbox.delivery.services.dto.PaymentDto

@RestController
@RequestMapping("/payment")
class PaymentController(
    private val paymentService: PaymentService,
    private val orderService: OrderService,
    private val bankService: BankService,
    private val messageService: MessageService,
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
        TODO()
//        val authentication = SecurityContextHolder.getContext().authentication
//        if (authentication.authorities.contains(SimpleGrantedAuthority("ROLE_ANONYMOUS"))) {
//            throw ResponseStatusException(HttpStatusCode.valueOf(401), "Access denied")
//        }
//
//        val userId = (SecurityContextHolder.getContext().authentication.principal as String).toLongOrNull()
//            ?: throw ResponseStatusException(HttpStatusCode.valueOf(401), "Access denied")
//
//        val ipAddress = request.getHeader("X-Forwarded-For")
//
//        val order = orderService.createOrder(userId!!)
//
//        if (body.amount != order!!.totalAmount) return ResponseEntity.ok(
//            PaymentDto(
//                success = false,
//                message = "Сумма отличается",
//                paymentType = body.paymentType
//            )
//        )
//
//        val paymentResponse = paymentService.pay(
//            paymentType = body.paymentType,
//            token = body.token,
//            cryptogram = body.cryptogram,
//            amount = body.amount,
//            userId = userId,
//            orderId = order.id,
//            ipAddress = if (ipAddress.isNullOrBlank() || ipAddress == "unknown") request.remoteAddr else ipAddress
//        )
//
//        if (paymentResponse.success && paymentResponse.model != null) {
//            val orderId = paymentResponse.model.orderId ?: throw ResponseStatusException(
//                HttpStatusCode.valueOf(404),
//                "Order not found"
//            )
//
//            if (paymentResponse.paymentType == "cash") {
//                val updatedOrder = orderService.updateOrderStatus(orderId, OrderStatus.PROCESSING)
//
//                if (updatedOrder != null) {
//                    messageService.sendMessageToBot(
//                        updatedOrder.id.toString(),
//                        updatedOrder.status
//                    )
//                }
//
//            } else {
//                orderService.updateOrderStatus(orderId, OrderStatus.AWAITING_PAYMENT)
//            }
//
//        }
//
//        return ResponseEntity.ok(paymentResponse)
    }

    @GetMapping("/getQrBanks")
    fun getQrBanks(): ResponseEntity<GetBanksResponse> {
        val banks = bankService.getQrBanks()
        val response = GetBanksResponse(banks)
        return ResponseEntity.ok(response)
    }

    @GetMapping("/getSubBanks")
    fun getSubBanks(): ResponseEntity<GetBanksResponse> {
        val banks = bankService.getSubBanks()
        val response = GetBanksResponse(banks)
        return ResponseEntity.ok(response)
    }

}