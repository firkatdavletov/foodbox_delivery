package ru.foodbox.delivery.modules.delivery.infrastructure.repository

import org.springframework.stereotype.Repository
import ru.foodbox.delivery.modules.delivery.domain.DeliveryOffer
import ru.foodbox.delivery.modules.delivery.domain.repository.DeliveryOfferRepository
import ru.foodbox.delivery.modules.delivery.infrastructure.persistence.entity.DeliveryOfferEntity
import ru.foodbox.delivery.modules.delivery.infrastructure.persistence.jpa.DeliveryOfferJpaRepository
import java.util.UUID
import kotlin.jvm.optionals.getOrNull

@Repository
class DeliveryOfferRepositoryImpl(
    private val jpaRepository: DeliveryOfferJpaRepository,
) : DeliveryOfferRepository {

    override fun save(offer: DeliveryOffer): DeliveryOffer {
        val existing = jpaRepository.findById(offer.id).getOrNull()
        val entity = existing ?: DeliveryOfferEntity(
            id = offer.id,
            provider = offer.provider,
            externalOfferId = offer.externalOfferId,
            expiresAt = offer.expiresAt,
            pricingMinor = offer.pricingMinor,
            pricingTotalMinor = offer.pricingTotalMinor,
            currency = offer.currency,
            commissionOnDeliveryPercent = offer.commissionOnDeliveryPercent,
            commissionOnDeliveryAmountMinor = offer.commissionOnDeliveryAmountMinor,
            deliveryPolicy = offer.deliveryPolicy,
            deliveryIntervalFrom = offer.deliveryIntervalFrom,
            deliveryIntervalTo = offer.deliveryIntervalTo,
            pickupIntervalFrom = offer.pickupIntervalFrom,
            pickupIntervalTo = offer.pickupIntervalTo,
            createdAt = offer.createdAt,
            updatedAt = offer.updatedAt,
        )

        entity.provider = offer.provider
        entity.externalOfferId = offer.externalOfferId
        entity.expiresAt = offer.expiresAt
        entity.pricingMinor = offer.pricingMinor
        entity.pricingTotalMinor = offer.pricingTotalMinor
        entity.currency = offer.currency
        entity.commissionOnDeliveryPercent = offer.commissionOnDeliveryPercent
        entity.commissionOnDeliveryAmountMinor = offer.commissionOnDeliveryAmountMinor
        entity.deliveryPolicy = offer.deliveryPolicy
        entity.deliveryIntervalFrom = offer.deliveryIntervalFrom
        entity.deliveryIntervalTo = offer.deliveryIntervalTo
        entity.pickupIntervalFrom = offer.pickupIntervalFrom
        entity.pickupIntervalTo = offer.pickupIntervalTo
        entity.updatedAt = offer.updatedAt

        return jpaRepository.save(entity).toDomain()
    }

    override fun findById(offerId: UUID): DeliveryOffer? {
        return jpaRepository.findById(offerId).getOrNull()?.toDomain()
    }

    private fun DeliveryOfferEntity.toDomain(): DeliveryOffer {
        return DeliveryOffer(
            id = id,
            provider = provider,
            externalOfferId = externalOfferId,
            expiresAt = expiresAt,
            pricingMinor = pricingMinor,
            pricingTotalMinor = pricingTotalMinor,
            currency = currency,
            commissionOnDeliveryPercent = commissionOnDeliveryPercent,
            commissionOnDeliveryAmountMinor = commissionOnDeliveryAmountMinor,
            deliveryPolicy = deliveryPolicy,
            deliveryIntervalFrom = deliveryIntervalFrom,
            deliveryIntervalTo = deliveryIntervalTo,
            pickupIntervalFrom = pickupIntervalFrom,
            pickupIntervalTo = pickupIntervalTo,
            createdAt = createdAt,
            updatedAt = updatedAt,
        )
    }
}
