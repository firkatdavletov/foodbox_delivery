package ru.foodbox.delivery.modules.payments.api.dto

import ru.foodbox.delivery.modules.payments.domain.PaymentMethodCode
import ru.foodbox.delivery.modules.payments.domain.PaymentStatus
import java.time.Instant
import java.util.UUID

data class PaymentResponse(
    val id: UUID,
    val orderId: UUID,
    val paymentMethodCode: PaymentMethodCode,
    val paymentMethodName: String,
    val status: PaymentStatus,
    val amountMinor: Long,
    val currency: String,
    val isOnline: Boolean,
    val providerCode: String?,
    val externalPaymentId: String?,
    val confirmationUrl: String?,
    val details: String?,
    val createdAt: Instant,
    val updatedAt: Instant,
    val paidAt: Instant?,
)
