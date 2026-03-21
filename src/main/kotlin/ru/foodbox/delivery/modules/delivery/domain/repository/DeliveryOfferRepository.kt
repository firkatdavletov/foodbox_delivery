package ru.foodbox.delivery.modules.delivery.domain.repository

import ru.foodbox.delivery.modules.delivery.domain.DeliveryOffer
import java.util.UUID

interface DeliveryOfferRepository {
    fun save(offer: DeliveryOffer): DeliveryOffer
    fun findById(offerId: UUID): DeliveryOffer?
}
