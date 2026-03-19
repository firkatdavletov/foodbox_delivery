package ru.foodbox.delivery.modules.delivery.application

import ru.foodbox.delivery.modules.delivery.domain.YandexDeliveryLocationVariant
import ru.foodbox.delivery.modules.delivery.domain.YandexPickupPointOption

interface YandexDeliveryGateway {
    fun isConfigured(): Boolean
    fun detectLocations(query: String): List<YandexDeliveryLocationVariant>
    fun listPickupPoints(geoId: Long): List<YandexPickupPointOption>
    fun getPickupPoint(pickupPointId: String): YandexPickupPointOption?
    fun calculateSelfPickupPrice(
        pickupPointId: String,
        subtotalMinor: Long,
        totalWeightGrams: Long? = null,
    ): YandexDeliveryPricingQuote
}

data class YandexDeliveryPricingQuote(
    val priceMinor: Long,
    val currency: String,
    val deliveryDays: Int?,
)
