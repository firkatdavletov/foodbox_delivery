package ru.foodbox.delivery.modules.payments.application.command

import ru.foodbox.delivery.modules.payments.domain.PaymentMethodCode
import java.util.UUID

data class CreatePaymentCommand(
    val orderId: UUID,
    val paymentMethodCode: PaymentMethodCode,
    val details: String?,
)
