package ru.foodbox.delivery.modules.delivery.domain

import java.time.Instant
import java.util.UUID

data class DeliveryOffer(
    val id: UUID,
    val provider: DeliveryOfferProvider,
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
    val createdAt: Instant,
    val updatedAt: Instant,
)
