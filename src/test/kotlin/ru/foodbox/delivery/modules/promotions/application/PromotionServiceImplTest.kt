package ru.foodbox.delivery.modules.promotions.application

import org.junit.jupiter.api.Test
import ru.foodbox.delivery.modules.promotions.application.command.ApplyOrderPromotionsCommand
import ru.foodbox.delivery.modules.promotions.domain.GiftCertificate
import ru.foodbox.delivery.modules.promotions.domain.GiftCertificateStatus
import ru.foodbox.delivery.modules.promotions.domain.GiftCertificateTransaction
import ru.foodbox.delivery.modules.promotions.domain.PromoCode
import ru.foodbox.delivery.modules.promotions.domain.PromoCodeDiscountType
import ru.foodbox.delivery.modules.promotions.domain.PromoCodeFilter
import ru.foodbox.delivery.modules.promotions.domain.PromoCodeRedemption
import ru.foodbox.delivery.modules.promotions.domain.repository.GiftCertificateRepository
import ru.foodbox.delivery.modules.promotions.domain.repository.PromoCodeRepository
import java.time.Clock
import java.time.Instant
import java.time.ZoneOffset
import java.util.UUID
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertNotNull

class PromotionServiceImplTest {

    private val now = Instant.parse("2026-05-05T12:00:00Z")
    private val clock: Clock = Clock.fixed(now, ZoneOffset.UTC)

    @Test
    fun `applies promo code and gift certificate and persists changes`() {
        val promoCode = PromoCode(
            id = UUID.randomUUID(),
            code = "PROMO500",
            discountType = PromoCodeDiscountType.FIXED,
            discountValue = 500,
            minOrderAmountMinor = null,
            maxDiscountMinor = null,
            currency = "RUB",
            startsAt = null,
            endsAt = null,
            usageLimitTotal = 10,
            usageLimitPerUser = 2,
            usedCount = 0,
            active = true,
            createdAt = now,
            updatedAt = now,
        )
        val giftCertificate = GiftCertificate(
            id = UUID.randomUUID(),
            code = "GIFT1200",
            initialAmountMinor = 1_200,
            balanceMinor = 1_200,
            currency = "RUB",
            status = GiftCertificateStatus.ACTIVE,
            expiresAt = null,
            createdAt = now,
            updatedAt = now,
        )
        val promoCodeRepository = InMemoryPromoCodeRepository(listOf(promoCode))
        val giftCertificateRepository = InMemoryGiftCertificateRepository(listOf(giftCertificate))
        val service = PromotionServiceImpl(
            promoCodeRepository = promoCodeRepository,
            giftCertificateRepository = giftCertificateRepository,
            clock = clock,
        )
        val userId = UUID.randomUUID()

        val result = service.applyOrderPromotions(
            ApplyOrderPromotionsCommand(
                orderId = UUID.randomUUID(),
                userId = userId,
                grossTotalMinor = 3_000,
                currency = "RUB",
                promoCode = " promo500 ",
                giftCertificateCode = "gift1200",
            )
        )

        assertEquals("PROMO500", result.promoCode)
        assertEquals(500, result.promoDiscountMinor)
        assertEquals(1_200, result.giftCertificateAmountMinor)
        assertEquals(1_300, result.totalMinor)
        assertEquals(giftCertificate.id, result.giftCertificateId)
        assertEquals("1200", result.giftCertificateCodeLast4)

        val savedPromoCode = promoCodeRepository.findByCodeForUpdate("PROMO500")
        assertNotNull(savedPromoCode)
        assertEquals(1, savedPromoCode.usedCount)
        assertEquals(1, promoCodeRepository.redemptions.size)

        val savedGiftCertificate = giftCertificateRepository.findByCodeForUpdate("GIFT1200")
        assertNotNull(savedGiftCertificate)
        assertEquals(0, savedGiftCertificate.balanceMinor)
        assertEquals(GiftCertificateStatus.USED, savedGiftCertificate.status)
        assertEquals(1, giftCertificateRepository.transactions.size)
    }

    @Test
    fun `fails when promo code per-user limit is reached`() {
        val userId = UUID.randomUUID()
        val promoCode = PromoCode(
            id = UUID.randomUUID(),
            code = "LIMITED",
            discountType = PromoCodeDiscountType.FIXED,
            discountValue = 100,
            minOrderAmountMinor = null,
            maxDiscountMinor = null,
            currency = null,
            startsAt = null,
            endsAt = null,
            usageLimitTotal = null,
            usageLimitPerUser = 1,
            usedCount = 0,
            active = true,
            createdAt = now,
            updatedAt = now,
        )
        val promoCodeRepository = InMemoryPromoCodeRepository(
            promoCodes = listOf(promoCode),
            userRedemptions = mapOf(promoCode.id to mapOf(userId to 1L)),
        )
        val service = PromotionServiceImpl(
            promoCodeRepository = promoCodeRepository,
            giftCertificateRepository = InMemoryGiftCertificateRepository(emptyList()),
            clock = clock,
        )

        assertFailsWith<IllegalArgumentException> {
            service.applyOrderPromotions(
                ApplyOrderPromotionsCommand(
                    orderId = UUID.randomUUID(),
                    userId = userId,
                    grossTotalMinor = 1_000,
                    currency = "RUB",
                    promoCode = "limited",
                )
            )
        }
    }

    private class InMemoryPromoCodeRepository(
        promoCodes: List<PromoCode>,
        private val userRedemptions: Map<UUID, Map<UUID, Long>> = emptyMap(),
    ) : PromoCodeRepository {
        private val byCode = promoCodes.associateBy { it.code }.toMutableMap()
        private val byId = promoCodes.associateBy { it.id }.toMutableMap()
        val redemptions = mutableListOf<PromoCodeRedemption>()

        override fun findAll(filter: PromoCodeFilter): List<PromoCode> {
            return byCode.values.toList()
        }

        override fun findById(id: UUID): PromoCode? {
            return byId[id]
        }

        override fun findByCode(code: String): PromoCode? {
            return byCode.values.firstOrNull { it.code.equals(code, ignoreCase = true) }
        }

        override fun findByCodeForUpdate(code: String): PromoCode? {
            return findByCode(code)
        }

        override fun save(promoCode: PromoCode): PromoCode {
            byCode[promoCode.code] = promoCode
            byId[promoCode.id] = promoCode
            return promoCode
        }

        override fun deleteById(id: UUID) {
            val removed = byId.remove(id) ?: return
            byCode.remove(removed.code)
        }

        override fun countUserRedemptions(promoCodeId: UUID, userId: UUID): Long {
            val inMemory = redemptions.count { it.promoCodeId == promoCodeId && it.userId == userId }.toLong()
            return inMemory + (userRedemptions[promoCodeId]?.get(userId) ?: 0L)
        }

        override fun saveRedemption(redemption: PromoCodeRedemption): PromoCodeRedemption {
            redemptions += redemption
            return redemption
        }
    }

    private class InMemoryGiftCertificateRepository(
        certificates: List<GiftCertificate>,
    ) : GiftCertificateRepository {
        private val byCode = certificates.associateBy { it.code }.toMutableMap()
        val transactions = mutableListOf<GiftCertificateTransaction>()

        override fun findByCodeForUpdate(code: String): GiftCertificate? {
            return byCode[code]
        }

        override fun save(certificate: GiftCertificate): GiftCertificate {
            byCode[certificate.code] = certificate
            return certificate
        }

        override fun saveTransaction(transaction: GiftCertificateTransaction): GiftCertificateTransaction {
            transactions += transaction
            return transaction
        }
    }
}
