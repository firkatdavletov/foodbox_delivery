package ru.foodbox.delivery.modules.delivery.infrastructure.repository

import org.springframework.stereotype.Repository
import ru.foodbox.delivery.modules.delivery.domain.OrderDeliveryOffer
import ru.foodbox.delivery.modules.delivery.domain.repository.OrderDeliveryOfferRepository
import ru.foodbox.delivery.modules.delivery.infrastructure.persistence.entity.OrderDeliveryOfferEntity
import ru.foodbox.delivery.modules.delivery.infrastructure.persistence.jpa.OrderDeliveryOfferJpaRepository
import java.util.UUID
import kotlin.jvm.optionals.getOrNull

@Repository
class OrderDeliveryOfferRepositoryImpl(
    private val jpaRepository: OrderDeliveryOfferJpaRepository,
) : OrderDeliveryOfferRepository {

    override fun save(link: OrderDeliveryOffer): OrderDeliveryOffer {
        val existing = jpaRepository.findById(link.id).getOrNull()
        val entity = existing ?: OrderDeliveryOfferEntity(
            id = link.id,
            orderId = link.orderId,
            offerId = link.offerId,
            externalRequestId = link.externalRequestId,
            confirmedAt = link.confirmedAt,
            createdAt = link.createdAt,
            updatedAt = link.updatedAt,
        )

        entity.orderId = link.orderId
        entity.offerId = link.offerId
        entity.externalRequestId = link.externalRequestId
        entity.confirmedAt = link.confirmedAt
        entity.updatedAt = link.updatedAt

        return jpaRepository.save(entity).toDomain()
    }

    override fun findByOrderId(orderId: UUID): OrderDeliveryOffer? {
        return jpaRepository.findByOrderId(orderId)?.toDomain()
    }

    private fun OrderDeliveryOfferEntity.toDomain(): OrderDeliveryOffer {
        return OrderDeliveryOffer(
            id = id,
            orderId = orderId,
            offerId = offerId,
            externalRequestId = externalRequestId,
            createdAt = createdAt,
            updatedAt = updatedAt,
            confirmedAt = confirmedAt,
        )
    }
}
