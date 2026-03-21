package ru.foodbox.delivery.modules.delivery.domain.repository

import ru.foodbox.delivery.modules.delivery.domain.OrderDeliveryOffer
import java.util.UUID

interface OrderDeliveryOfferRepository {
    fun save(link: OrderDeliveryOffer): OrderDeliveryOffer
    fun findByOrderId(orderId: UUID): OrderDeliveryOffer?
}
