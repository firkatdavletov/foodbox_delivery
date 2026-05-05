package ru.foodbox.delivery.modules.promotions.infrastructure.repository

import org.springframework.data.jpa.domain.Specification
import org.springframework.stereotype.Repository
import ru.foodbox.delivery.modules.promotions.domain.PromoCode
import ru.foodbox.delivery.modules.promotions.domain.PromoCodeFilter
import ru.foodbox.delivery.modules.promotions.domain.PromoCodeRedemption
import ru.foodbox.delivery.modules.promotions.domain.repository.PromoCodeRepository
import ru.foodbox.delivery.modules.promotions.infrastructure.persistence.entity.PromoCodeEntity
import ru.foodbox.delivery.modules.promotions.infrastructure.persistence.entity.PromoCodeRedemptionEntity
import ru.foodbox.delivery.modules.promotions.infrastructure.persistence.jpa.PromoCodeJpaRepository
import ru.foodbox.delivery.modules.promotions.infrastructure.persistence.jpa.PromoCodeRedemptionJpaRepository
import java.time.Instant
import kotlin.jvm.optionals.getOrNull
import java.util.UUID

@Repository
class PromoCodeRepositoryImpl(
    private val promoCodeJpaRepository: PromoCodeJpaRepository,
    private val promoCodeRedemptionJpaRepository: PromoCodeRedemptionJpaRepository,
) : PromoCodeRepository {

    override fun findAll(filter: PromoCodeFilter): List<PromoCode> {
        val specification = Specification<PromoCodeEntity> { root, _, criteriaBuilder ->
            val predicates = mutableListOf<jakarta.persistence.criteria.Predicate>()

            filter.active?.let { active ->
                predicates += criteriaBuilder.equal(root.get<Boolean>("active"), active)
            }
            filter.discountType?.let { discountType ->
                predicates += criteriaBuilder.equal(root.get<Any>("discountType"), discountType)
            }
            filter.code?.trim()?.takeIf { it.isNotBlank() }?.let { code ->
                predicates += criteriaBuilder.like(
                    criteriaBuilder.upper(root.get("code")),
                    "%${code.uppercase()}%",
                )
            }
            filter.validAt?.let { validAt ->
                predicates += criteriaBuilder.or(
                    criteriaBuilder.isNull(root.get<Instant>("startsAt")),
                    criteriaBuilder.lessThanOrEqualTo(root.get<Instant>("startsAt"), validAt),
                )
                predicates += criteriaBuilder.or(
                    criteriaBuilder.isNull(root.get<Instant>("endsAt")),
                    criteriaBuilder.greaterThanOrEqualTo(root.get<Instant>("endsAt"), validAt),
                )
            }

            criteriaBuilder.and(*predicates.toTypedArray())
        }

        return promoCodeJpaRepository.findAll(specification)
            .sortedWith(compareByDescending<PromoCodeEntity> { it.createdAt }.thenBy { it.code })
            .map { it.toDomain() }
    }

    override fun findById(id: UUID): PromoCode? {
        return promoCodeJpaRepository.findById(id).getOrNull()?.toDomain()
    }

    override fun findByCode(code: String): PromoCode? {
        return promoCodeJpaRepository.findByCodeIgnoreCase(code)?.toDomain()
    }

    override fun findByCodeForUpdate(code: String): PromoCode? {
        return promoCodeJpaRepository.findByCodeForUpdate(code)?.toDomain()
    }

    override fun save(promoCode: PromoCode): PromoCode {
        val existing = promoCodeJpaRepository.findById(promoCode.id).getOrNull()
        val entity = existing ?: PromoCodeEntity(
            id = promoCode.id,
            code = promoCode.code,
            discountType = promoCode.discountType,
            discountValue = promoCode.discountValue,
            minOrderAmountMinor = promoCode.minOrderAmountMinor,
            maxDiscountMinor = promoCode.maxDiscountMinor,
            currency = promoCode.currency,
            startsAt = promoCode.startsAt,
            endsAt = promoCode.endsAt,
            usageLimitTotal = promoCode.usageLimitTotal,
            usageLimitPerUser = promoCode.usageLimitPerUser,
            usedCount = promoCode.usedCount,
            active = promoCode.active,
            createdAt = promoCode.createdAt,
            updatedAt = promoCode.updatedAt,
        )

        entity.code = promoCode.code
        entity.discountType = promoCode.discountType
        entity.discountValue = promoCode.discountValue
        entity.minOrderAmountMinor = promoCode.minOrderAmountMinor
        entity.maxDiscountMinor = promoCode.maxDiscountMinor
        entity.currency = promoCode.currency
        entity.startsAt = promoCode.startsAt
        entity.endsAt = promoCode.endsAt
        entity.usageLimitTotal = promoCode.usageLimitTotal
        entity.usageLimitPerUser = promoCode.usageLimitPerUser
        entity.usedCount = promoCode.usedCount
        entity.active = promoCode.active
        entity.updatedAt = promoCode.updatedAt

        return promoCodeJpaRepository.save(entity).toDomain()
    }

    override fun deleteById(id: UUID) {
        promoCodeJpaRepository.deleteById(id)
    }

    override fun countUserRedemptions(promoCodeId: UUID, userId: UUID): Long {
        return promoCodeRedemptionJpaRepository.countByPromoCodeIdAndUserId(
            promoCodeId = promoCodeId,
            userId = userId,
        )
    }

    override fun saveRedemption(redemption: PromoCodeRedemption): PromoCodeRedemption {
        val entity = PromoCodeRedemptionEntity(
            id = redemption.id,
            promoCodeId = redemption.promoCodeId,
            orderId = redemption.orderId,
            userId = redemption.userId,
            discountMinor = redemption.discountMinor,
            createdAt = redemption.createdAt,
        )

        return promoCodeRedemptionJpaRepository.save(entity).toDomain()
    }

    private fun PromoCodeEntity.toDomain(): PromoCode {
        return PromoCode(
            id = id,
            code = code,
            discountType = discountType,
            discountValue = discountValue,
            minOrderAmountMinor = minOrderAmountMinor,
            maxDiscountMinor = maxDiscountMinor,
            currency = currency,
            startsAt = startsAt,
            endsAt = endsAt,
            usageLimitTotal = usageLimitTotal,
            usageLimitPerUser = usageLimitPerUser,
            usedCount = usedCount,
            active = active,
            createdAt = createdAt,
            updatedAt = updatedAt,
        )
    }

    private fun PromoCodeRedemptionEntity.toDomain(): PromoCodeRedemption {
        return PromoCodeRedemption(
            id = id,
            promoCodeId = promoCodeId,
            orderId = orderId,
            userId = userId,
            discountMinor = discountMinor,
            createdAt = createdAt,
        )
    }
}
