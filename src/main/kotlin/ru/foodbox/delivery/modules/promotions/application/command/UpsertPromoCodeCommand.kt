package ru.foodbox.delivery.modules.promotions.application.command

import ru.foodbox.delivery.modules.promotions.domain.PromoCodeDiscountType
import java.time.Instant

data class UpsertPromoCodeCommand(
    val code: String,
    val discountType: PromoCodeDiscountType,
    val discountValue: Long,
    val minOrderAmountMinor: Long?,
    val maxDiscountMinor: Long?,
    val currency: String?,
    val startsAt: Instant?,
    val endsAt: Instant?,
    val usageLimitTotal: Int?,
    val usageLimitPerUser: Int?,
    val active: Boolean,
)
