package ru.foodbox.delivery.modules.payments.api

import jakarta.validation.Valid
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController
import ru.foodbox.delivery.common.web.CurrentActor
import ru.foodbox.delivery.common.web.CurrentActorParam
import ru.foodbox.delivery.modules.payments.api.dto.CreatePaymentRequest
import ru.foodbox.delivery.modules.payments.api.dto.PaymentMethodResponse
import ru.foodbox.delivery.modules.payments.api.dto.PaymentResponse
import ru.foodbox.delivery.modules.payments.application.PaymentService
import ru.foodbox.delivery.modules.payments.application.command.CreatePaymentCommand
import java.util.UUID

@RestController
@RequestMapping("/api/v1/payments")
class PaymentController(
    private val paymentService: PaymentService,
) {

    @GetMapping("/methods")
    fun getAvailableMethods(): List<PaymentMethodResponse> {
        return paymentService.getAvailableMethods().map { it.toResponse() }
    }

    @PostMapping
    fun createPayment(
        @CurrentActorParam actor: CurrentActor,
        @Valid @RequestBody request: CreatePaymentRequest,
    ): PaymentResponse {
        return paymentService.createPayment(
            actor = actor,
            command = CreatePaymentCommand(
                orderId = request.orderId,
                paymentMethodCode = request.paymentMethodCode,
                details = request.details,
            ),
        ).toResponse()
    }

    @GetMapping("/{paymentId}")
    fun getPayment(
        @CurrentActorParam actor: CurrentActor,
        @PathVariable paymentId: UUID,
    ): PaymentResponse {
        return paymentService.getPayment(actor, paymentId).toResponse()
    }
}
