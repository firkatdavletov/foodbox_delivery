package ru.foodbox.delivery.modules.promotions.domain

import java.time.Instant
import java.util.UUID

data class PromoCodeRedemption(
    val id: UUID,
    val promoCodeId: UUID,
    val orderId: UUID,
    val userId: UUID?,
    val discountMinor: Long,
    val createdAt: Instant,
)
