package ru.foodbox.delivery.modules.promotions.application

import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import ru.foodbox.delivery.modules.promotions.application.command.ApplyOrderPromotionsCommand
import ru.foodbox.delivery.modules.promotions.domain.GiftCertificateTransaction
import ru.foodbox.delivery.modules.promotions.domain.GiftCertificateTransactionType
import ru.foodbox.delivery.modules.promotions.domain.PromoCodeDiscountType
import ru.foodbox.delivery.modules.promotions.domain.PromoCodeRedemption
import ru.foodbox.delivery.modules.promotions.domain.repository.GiftCertificateRepository
import ru.foodbox.delivery.modules.promotions.domain.repository.PromoCodeRepository
import java.time.Clock
import java.util.UUID

@Service
class PromotionServiceImpl(
    private val promoCodeRepository: PromoCodeRepository,
    private val giftCertificateRepository: GiftCertificateRepository,
    private val clock: Clock,
) : PromotionService {

    @Transactional
    override fun applyOrderPromotions(command: ApplyOrderPromotionsCommand): OrderPricingAdjustment {
        require(command.grossTotalMinor >= 0) { "grossTotalMinor must be non-negative" }

        val now = clock.instant()
        val normalizedPromoCode = normalizeCode(command.promoCode)
        val normalizedGiftCertificateCode = normalizeCode(command.giftCertificateCode)

        var promoDiscountMinor = 0L
        var appliedPromoCode: String? = null
        if (normalizedPromoCode != null) {
            val promoCode = promoCodeRepository.findByCodeForUpdate(normalizedPromoCode)
                ?: throw IllegalArgumentException("Promo code is invalid")

            validatePromoDiscountConfig(promoCode.discountType, promoCode.discountValue)
            promoCode.validateAvailability(
                orderAmountMinor = command.grossTotalMinor,
                orderCurrency = command.currency,
                now = now,
            )

            promoCode.usageLimitPerUser?.let { perUserLimit ->
                val userId = command.userId
                    ?: throw IllegalArgumentException("Promo code requires authenticated user")
                val userRedemptions = promoCodeRepository.countUserRedemptions(promoCode.id, userId)
                require(userRedemptions < perUserLimit) { "Promo code per-user usage limit reached" }
            }

            promoDiscountMinor = promoCode.calculateDiscount(command.grossTotalMinor)
            require(promoDiscountMinor > 0L) { "Promo code does not provide discount for current order" }

            promoCode.markRedeemed(now)
            promoCodeRepository.save(promoCode)
            promoCodeRepository.saveRedemption(
                PromoCodeRedemption(
                    id = UUID.randomUUID(),
                    promoCodeId = promoCode.id,
                    orderId = command.orderId,
                    userId = command.userId,
                    discountMinor = promoDiscountMinor,
                    createdAt = now,
                )
            )
            appliedPromoCode = promoCode.code
        }

        val amountAfterPromo = command.grossTotalMinor - promoDiscountMinor
        var giftCertificateId: UUID? = null
        var giftCertificateCodeLast4: String? = null
        var giftCertificateAmountMinor = 0L
        if (normalizedGiftCertificateCode != null && amountAfterPromo > 0L) {
            val giftCertificate = giftCertificateRepository.findByCodeForUpdate(normalizedGiftCertificateCode)
                ?: throw IllegalArgumentException("Gift certificate is invalid")

            giftCertificate.validateAvailability(
                orderCurrency = command.currency,
                now = now,
            )
            giftCertificateAmountMinor = giftCertificate.apply(
                requestedAmountMinor = amountAfterPromo,
                now = now,
            )
            require(giftCertificateAmountMinor > 0L) { "Gift certificate does not have sufficient balance" }

            val savedGiftCertificate = giftCertificateRepository.save(giftCertificate)
            giftCertificateRepository.saveTransaction(
                GiftCertificateTransaction(
                    id = UUID.randomUUID(),
                    giftCertificateId = savedGiftCertificate.id,
                    orderId = command.orderId,
                    type = GiftCertificateTransactionType.DEBIT,
                    amountMinor = giftCertificateAmountMinor,
                    createdAt = now,
                )
            )

            giftCertificateId = savedGiftCertificate.id
            giftCertificateCodeLast4 = savedGiftCertificate.codeLast4()
        }

        val totalMinor = command.grossTotalMinor - promoDiscountMinor - giftCertificateAmountMinor
        require(totalMinor >= 0) { "Total after promotions must be non-negative" }

        return OrderPricingAdjustment(
            promoCode = appliedPromoCode,
            promoDiscountMinor = promoDiscountMinor,
            giftCertificateId = giftCertificateId,
            giftCertificateCodeLast4 = giftCertificateCodeLast4,
            giftCertificateAmountMinor = giftCertificateAmountMinor,
            totalMinor = totalMinor,
        )
    }

    private fun normalizeCode(rawCode: String?): String? {
        return rawCode?.trim()?.uppercase()?.takeIf { it.isNotBlank() }
    }

    private fun validatePromoDiscountConfig(
        discountType: PromoCodeDiscountType,
        discountValue: Long,
    ) {
        require(discountValue > 0) { "Promo code discount value must be positive" }
        if (discountType == PromoCodeDiscountType.PERCENT) {
            require(discountValue <= 100) { "Promo code percent must be in range 1..100" }
        }
    }
}
