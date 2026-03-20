package ru.foodbox.delivery.modules.payments.domain

import java.time.Instant
import java.util.UUID

data class Payment(
    val id: UUID,
    val orderId: UUID,
    val paymentMethodCode: PaymentMethodCode,
    val paymentMethodName: String,
    var status: PaymentStatus,
    val amountMinor: Long,
    val currency: String,
    val providerCode: String?,
    val externalPaymentId: String?,
    val confirmationUrl: String?,
    val details: String?,
    val createdAt: Instant,
    var updatedAt: Instant,
    val paidAt: Instant?,
) {
    fun isActive(): Boolean = !status.isTerminal()
}
