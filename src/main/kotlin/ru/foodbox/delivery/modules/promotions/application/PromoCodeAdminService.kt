package ru.foodbox.delivery.modules.promotions.application

import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import ru.foodbox.delivery.common.error.ConflictException
import ru.foodbox.delivery.common.error.NotFoundException
import ru.foodbox.delivery.modules.promotions.application.command.UpsertPromoCodeCommand
import ru.foodbox.delivery.modules.promotions.domain.PromoCode
import ru.foodbox.delivery.modules.promotions.domain.PromoCodeDiscountType
import ru.foodbox.delivery.modules.promotions.domain.PromoCodeFilter
import ru.foodbox.delivery.modules.promotions.domain.repository.PromoCodeRepository
import java.time.Clock
import java.util.UUID

@Service
class PromoCodeAdminService(
    private val promoCodeRepository: PromoCodeRepository,
    private val clock: Clock,
) {

    @Transactional(readOnly = true)
    fun getPromoCodes(filter: PromoCodeFilter): List<PromoCode> {
        return promoCodeRepository.findAll(
            filter.copy(
                code = filter.code?.trim()?.takeIf { it.isNotBlank() },
            )
        )
    }

    @Transactional(readOnly = true)
    fun getPromoCode(id: UUID): PromoCode {
        return promoCodeRepository.findById(id)
            ?: throw NotFoundException("Promo code not found")
    }

    @Transactional(readOnly = true)
    fun searchPromoCode(code: String): PromoCode {
        val normalizedCode = normalizeCode(code)
        return promoCodeRepository.findByCode(normalizedCode)
            ?: throw NotFoundException("Promo code not found")
    }

    @Transactional
    fun createPromoCode(command: UpsertPromoCodeCommand): PromoCode {
        validateCommand(command)
        val normalizedCode = normalizeCode(command.code)
        ensureCodeUniqueness(normalizedCode, currentPromoCodeId = null)
        val now = clock.instant()

        return promoCodeRepository.save(
            PromoCode(
                id = UUID.randomUUID(),
                code = normalizedCode,
                discountType = command.discountType,
                discountValue = command.discountValue,
                minOrderAmountMinor = command.minOrderAmountMinor,
                maxDiscountMinor = command.maxDiscountMinor,
                currency = normalizeCurrency(command.currency),
                startsAt = command.startsAt,
                endsAt = command.endsAt,
                usageLimitTotal = command.usageLimitTotal,
                usageLimitPerUser = command.usageLimitPerUser,
                usedCount = 0,
                active = command.active,
                createdAt = now,
                updatedAt = now,
            )
        )
    }

    @Transactional
    fun updatePromoCode(
        promoCodeId: UUID,
        command: UpsertPromoCodeCommand,
    ): PromoCode {
        validateCommand(command)
        val existing = promoCodeRepository.findById(promoCodeId)
            ?: throw NotFoundException("Promo code not found")
        val normalizedCode = normalizeCode(command.code)
        ensureCodeUniqueness(normalizedCode, currentPromoCodeId = promoCodeId)
        val now = clock.instant()

        return promoCodeRepository.save(
            existing.copy(
                code = normalizedCode,
                discountType = command.discountType,
                discountValue = command.discountValue,
                minOrderAmountMinor = command.minOrderAmountMinor,
                maxDiscountMinor = command.maxDiscountMinor,
                currency = normalizeCurrency(command.currency),
                startsAt = command.startsAt,
                endsAt = command.endsAt,
                usageLimitTotal = command.usageLimitTotal,
                usageLimitPerUser = command.usageLimitPerUser,
                active = command.active,
                updatedAt = now,
            )
        )
    }

    @Transactional
    fun deletePromoCode(promoCodeId: UUID) {
        val existing = promoCodeRepository.findById(promoCodeId)
            ?: throw NotFoundException("Promo code not found")
        promoCodeRepository.deleteById(existing.id)
    }

    private fun validateCommand(command: UpsertPromoCodeCommand) {
        require(command.discountValue > 0) { "discountValue must be positive" }
        if (command.discountType == PromoCodeDiscountType.PERCENT) {
            require(command.discountValue in 1..100) { "discountValue for PERCENT must be between 1 and 100" }
        }
        command.minOrderAmountMinor?.let {
            require(it >= 0) { "minOrderAmountMinor must be non-negative" }
        }
        command.maxDiscountMinor?.let {
            require(it >= 0) { "maxDiscountMinor must be non-negative" }
        }
        command.usageLimitTotal?.let {
            require(it > 0) { "usageLimitTotal must be positive" }
        }
        command.usageLimitPerUser?.let {
            require(it > 0) { "usageLimitPerUser must be positive" }
        }
        require(
            command.startsAt == null || command.endsAt == null || !command.startsAt.isAfter(command.endsAt)
        ) { "startsAt must be less than or equal to endsAt" }
        normalizeCurrency(command.currency)
    }

    private fun ensureCodeUniqueness(
        code: String,
        currentPromoCodeId: UUID?,
    ) {
        val existingWithCode = promoCodeRepository.findByCode(code) ?: return
        if (existingWithCode.id != currentPromoCodeId) {
            throw ConflictException("Promo code with the same code already exists")
        }
    }

    private fun normalizeCode(code: String): String {
        return code.trim()
            .takeIf { it.isNotBlank() }
            ?.uppercase()
            ?: throw IllegalArgumentException("code must not be blank")
    }

    private fun normalizeCurrency(currency: String?): String? {
        val normalized = currency?.trim()?.takeIf { it.isNotBlank() }?.uppercase() ?: return null
        require(normalized.length == 3) { "currency must be ISO-4217 code" }
        return normalized
    }
}
