package ru.foodbox.delivery.modules.delivery.application

import ru.foodbox.delivery.modules.delivery.domain.DeliveryAddress

interface DeliveryAddressGeocoder {
    fun isConfigured(): Boolean
    fun reverseGeocode(latitude: Double, longitude: Double): DeliveryAddress?
}
