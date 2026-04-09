package ru.foodbox.delivery.modules.delivery.application

import ru.foodbox.delivery.modules.delivery.domain.DeliveryMethodSetting
import ru.foodbox.delivery.modules.delivery.domain.DeliveryMethodType
import ru.foodbox.delivery.modules.delivery.domain.DeliveryQuote
import ru.foodbox.delivery.modules.delivery.domain.DeliveryQuoteContext
import ru.foodbox.delivery.modules.delivery.domain.PickupPoint
import ru.foodbox.delivery.modules.delivery.domain.YandexDeliveryLocationVariant
import ru.foodbox.delivery.modules.delivery.domain.YandexPickupPointOption

interface DeliveryService {
    fun getAvailableMethodSettings(): List<DeliveryMethodSetting>
    fun getAvailableMethods(): List<DeliveryMethodType> = getAvailableMethodSettings().map(DeliveryMethodSetting::method)
    fun getActivePickupPoints(): List<PickupPoint>
    fun detectYandexCity(latitude: Double, longitude: Double): String?
    fun detectYandexLocations(query: String): List<YandexDeliveryLocationVariant>
    fun getYandexPickupPoints(geoId: Long): List<YandexPickupPointOption>
    fun getYandexPickupPoint(pickupPointId: String): YandexPickupPointOption?
    fun calculateQuote(context: DeliveryQuoteContext): DeliveryQuote
}
