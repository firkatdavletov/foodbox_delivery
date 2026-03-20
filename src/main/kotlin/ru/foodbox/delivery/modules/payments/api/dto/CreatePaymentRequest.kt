package ru.foodbox.delivery.modules.payments.api.dto

import jakarta.validation.constraints.NotNull
import jakarta.validation.constraints.Size
import ru.foodbox.delivery.modules.payments.domain.PaymentMethodCode
import java.util.UUID

data class CreatePaymentRequest(
    @field:NotNull
    val orderId: UUID,

    @field:NotNull
    val paymentMethodCode: PaymentMethodCode,

    @field:Size(max = 2000)
    val details: String? = null,
)
