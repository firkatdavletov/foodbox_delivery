package ru.foodbox.delivery.modules.promotions.application

import java.util.UUID

data class OrderPricingAdjustment(
    val promoCode: String?,
    val promoDiscountMinor: Long,
    val giftCertificateId: UUID?,
    val giftCertificateCodeLast4: String?,
    val giftCertificateAmountMinor: Long,
    val totalMinor: Long,
)
