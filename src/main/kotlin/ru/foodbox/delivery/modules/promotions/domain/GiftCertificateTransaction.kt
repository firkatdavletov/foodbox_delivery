package ru.foodbox.delivery.modules.promotions.domain

import java.time.Instant
import java.util.UUID

data class GiftCertificateTransaction(
    val id: UUID,
    val giftCertificateId: UUID,
    val orderId: UUID,
    val type: GiftCertificateTransactionType,
    val amountMinor: Long,
    val createdAt: Instant,
)
