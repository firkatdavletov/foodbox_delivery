package ru.foodbox.delivery.modules.payments.application.port

import ru.foodbox.delivery.common.web.CurrentActor
import ru.foodbox.delivery.modules.payments.domain.PaymentMethodCode
import java.util.UUID

interface OrderPaymentPort {
    fun requireAccessibleOrder(actor: CurrentActor, orderId: UUID): PaymentOrderContext
    fun applyPaymentSnapshot(orderId: UUID, snapshot: PaymentOrderSnapshot)
}

data class PaymentOrderContext(
    val id: UUID,
    val totalMinor: Long,
    val currency: String,
)

data class PaymentOrderSnapshot(
    val paymentMethodCode: PaymentMethodCode,
    val paymentMethodName: String,
)
