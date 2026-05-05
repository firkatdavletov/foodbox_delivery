package ru.foodbox.delivery.modules.promotions.application.command

import java.util.UUID

data class ApplyOrderPromotionsCommand(
    val orderId: UUID,
    val userId: UUID?,
    val grossTotalMinor: Long,
    val currency: String,
    val promoCode: String? = null,
    val giftCertificateCode: String? = null,
)
