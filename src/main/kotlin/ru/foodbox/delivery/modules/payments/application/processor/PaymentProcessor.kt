package ru.foodbox.delivery.modules.payments.application.processor

import ru.foodbox.delivery.modules.payments.application.command.CreatePaymentCommand
import ru.foodbox.delivery.modules.payments.application.port.PaymentOrderContext
import ru.foodbox.delivery.modules.payments.domain.PaymentMethodCode
import ru.foodbox.delivery.modules.payments.domain.PaymentMethodInfo
import ru.foodbox.delivery.modules.payments.domain.PaymentStatus

interface PaymentProcessor {
    fun supports(method: PaymentMethodCode): Boolean
    fun getAvailableMethods(): List<PaymentMethodInfo>

    fun validate(
        command: CreatePaymentCommand,
        order: PaymentOrderContext,
    ) {
    }

    fun preparePayment(
        command: CreatePaymentCommand,
        order: PaymentOrderContext,
    ): PreparedPayment
}

data class PreparedPayment(
    val status: PaymentStatus,
    val providerCode: String? = null,
    val externalPaymentId: String? = null,
    val confirmationUrl: String? = null,
    val details: String? = null,
)
