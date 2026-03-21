package ru.foodbox.delivery.modules.delivery.application

import ru.foodbox.delivery.modules.delivery.domain.YandexDeliveryLocationVariant
import ru.foodbox.delivery.modules.delivery.domain.YandexPickupPointOption
import java.time.Instant

interface YandexDeliveryGateway {
    fun isConfigured(): Boolean
    fun detectLocations(query: String): List<YandexDeliveryLocationVariant>
    fun listPickupPoints(geoId: Long, paymentMethod: String? = null): List<YandexPickupPointOption>
    fun getPickupPoint(pickupPointId: String): YandexPickupPointOption?
    fun calculateSelfPickupPrice(
        pickupPointId: String,
        subtotalMinor: Long,
        totalWeightGrams: Long? = null,
    ): YandexDeliveryPricingQuote
    fun createOffers(request: YandexOfferCreateRequest): List<YandexDeliveryOffer>
    fun confirmOffer(offerId: String): YandexConfirmedDeliveryRequest
}

data class YandexDeliveryPricingQuote(
    val priceMinor: Long,
    val currency: String,
    val deliveryDays: Int?,
)

data class YandexOfferCreateRequest(
    val operatorRequestId: String,
    val comment: String?,
    val destinationPickupPointId: String,
    val recipientName: String?,
    val recipientPhone: String,
    val recipientEmail: String?,
    val items: List<YandexOfferItem>,
    val paymentMethod: YandexOfferPaymentMethod,
    val deliveryCostMinor: Long,
    val pickupIntervalFrom: Instant,
    val pickupIntervalTo: Instant,
)

data class YandexOfferItem(
    val id: String,
    val name: String,
    val article: String?,
    val count: Int,
    val unitPriceMinor: Long,
)

enum class YandexOfferPaymentMethod(
    val apiCode: String,
) {
    ALREADY_PAID("already_paid"),
    CARD_ON_RECEIPT("card_on_receipt"),
}

data class YandexDeliveryOffer(
    val externalOfferId: String,
    val expiresAt: Instant?,
    val pricingMinor: Long?,
    val pricingTotalMinor: Long?,
    val currency: String?,
    val commissionOnDeliveryPercent: String?,
    val commissionOnDeliveryAmountMinor: Long?,
    val deliveryPolicy: String?,
    val deliveryIntervalFrom: Instant?,
    val deliveryIntervalTo: Instant?,
    val pickupIntervalFrom: Instant?,
    val pickupIntervalTo: Instant?,
)

data class YandexConfirmedDeliveryRequest(
    val externalRequestId: String,
)
