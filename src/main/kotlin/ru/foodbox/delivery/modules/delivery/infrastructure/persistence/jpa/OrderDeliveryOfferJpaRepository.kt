package ru.foodbox.delivery.modules.delivery.infrastructure.persistence.jpa

import org.springframework.data.jpa.repository.JpaRepository
import ru.foodbox.delivery.modules.delivery.infrastructure.persistence.entity.OrderDeliveryOfferEntity
import java.util.UUID

interface OrderDeliveryOfferJpaRepository : JpaRepository<OrderDeliveryOfferEntity, UUID> {
    fun findByOrderId(orderId: UUID): OrderDeliveryOfferEntity?
}
