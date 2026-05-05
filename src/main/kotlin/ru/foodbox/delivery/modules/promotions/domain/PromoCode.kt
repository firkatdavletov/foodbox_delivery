package ru.foodbox.delivery.modules.promotions.domain

import java.time.Instant
import java.util.UUID
import kotlin.math.min

data class PromoCode(
    val id: UUID,
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
    var usedCount: Int,
    var active: Boolean,
    val createdAt: Instant,
    var updatedAt: Instant,
) {
    fun validateAvailability(
        orderAmountMinor: Long,
        orderCurrency: String,
        now: Instant,
    ) {
        require(active) { "Promo code is inactive" }
        require(startsAt == null || !now.isBefore(startsAt)) { "Promo code is not active yet" }
        require(endsAt == null || !now.isAfter(endsAt)) { "Promo code is expired" }
        require(minOrderAmountMinor == null || orderAmountMinor >= minOrderAmountMinor) {
            "Order amount is below promo minimum threshold"
        }
        require(currency == null || currency.equals(orderCurrency, ignoreCase = true)) {
            "Promo code currency does not match order currency"
        }
        require(usageLimitTotal == null || usedCount < usageLimitTotal) {
            "Promo code usage limit reached"
        }
    }

    fun calculateDiscount(orderAmountMinor: Long): Long {
        val baseDiscount = when (discountType) {
            PromoCodeDiscountType.FIXED -> discountValue
            PromoCodeDiscountType.PERCENT -> orderAmountMinor * discountValue / 100
        }
        require(baseDiscount >= 0) { "Promo code discount must be non-negative" }
        val cappedByPromo = maxDiscountMinor?.let { min(baseDiscount, it) } ?: baseDiscount
        return min(cappedByPromo, orderAmountMinor)
    }

    fun markRedeemed(now: Instant) {
        usedCount += 1
        updatedAt = now
    }
}
