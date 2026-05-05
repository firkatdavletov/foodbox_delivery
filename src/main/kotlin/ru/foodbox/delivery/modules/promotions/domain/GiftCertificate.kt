package ru.foodbox.delivery.modules.promotions.domain

import java.time.Instant
import java.util.UUID
import kotlin.math.min

data class GiftCertificate(
    val id: UUID,
    val code: String,
    val initialAmountMinor: Long,
    var balanceMinor: Long,
    val currency: String,
    var status: GiftCertificateStatus,
    val expiresAt: Instant?,
    val createdAt: Instant,
    var updatedAt: Instant,
) {
    fun validateAvailability(
        orderCurrency: String,
        now: Instant,
    ) {
        require(status != GiftCertificateStatus.BLOCKED) { "Gift certificate is blocked" }
        require(status != GiftCertificateStatus.EXPIRED) { "Gift certificate is expired" }
        require(currency.equals(orderCurrency, ignoreCase = true)) {
            "Gift certificate currency does not match order currency"
        }
        require(expiresAt == null || !now.isAfter(expiresAt)) { "Gift certificate is expired" }
        require(balanceMinor > 0) { "Gift certificate balance is empty" }
    }

    fun apply(
        requestedAmountMinor: Long,
        now: Instant,
    ): Long {
        require(requestedAmountMinor >= 0) { "Requested amount must be non-negative" }
        val debitedAmount = min(balanceMinor, requestedAmountMinor)
        if (debitedAmount == 0L) {
            return 0L
        }

        balanceMinor -= debitedAmount
        status = if (balanceMinor == 0L) {
            GiftCertificateStatus.USED
        } else {
            GiftCertificateStatus.ACTIVE
        }
        updatedAt = now
        return debitedAmount
    }

    fun codeLast4(): String {
        return code.takeLast(4)
    }
}
