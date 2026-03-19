package ru.foodbox.delivery.modules.delivery.domain.repository

import ru.foodbox.delivery.modules.delivery.domain.DeliveryZone

interface DeliveryZoneRepository {
    fun findActiveByCity(city: String): DeliveryZone?
    fun findActiveByPostalCode(postalCode: String): DeliveryZone?
}
