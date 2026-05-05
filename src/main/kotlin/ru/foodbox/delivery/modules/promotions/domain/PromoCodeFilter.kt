package ru.foodbox.delivery.modules.promotions.domain

import java.time.Instant

data class PromoCodeFilter(
    val active: Boolean? = null,
    val discountType: PromoCodeDiscountType? = null,
    val code: String? = null,
    val validAt: Instant? = null,
)
