package ru.foodbox.delivery.modules.delivery.infrastructure.persistence.entity

import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.EnumType
import jakarta.persistence.Enumerated
import jakarta.persistence.Id
import jakarta.persistence.Table
import ru.foodbox.delivery.modules.delivery.domain.DeliveryOfferProvider
import java.time.Instant
import java.util.UUID

@Entity
@Table(name = "delivery_offers")
class DeliveryOfferEntity(
    @Id
    @Column(nullable = false)
    var id: UUID,

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 32)
    var provider: DeliveryOfferProvider,

    @Column(name = "external_offer_id", nullable = false, unique = true, length = 128)
    var externalOfferId: String,

    @Column(name = "expires_at")
    var expiresAt: Instant? = null,

    @Column(name = "pricing_minor")
    var pricingMinor: Long? = null,

    @Column(name = "pricing_total_minor")
    var pricingTotalMinor: Long? = null,

    @Column(name = "currency", length = 3)
    var currency: String? = null,

    @Column(name = "commission_on_delivery_percent", length = 32)
    var commissionOnDeliveryPercent: String? = null,

    @Column(name = "commission_on_delivery_amount_minor")
    var commissionOnDeliveryAmountMinor: Long? = null,

    @Column(name = "delivery_policy", length = 64)
    var deliveryPolicy: String? = null,

    @Column(name = "delivery_interval_from")
    var deliveryIntervalFrom: Instant? = null,

    @Column(name = "delivery_interval_to")
    var deliveryIntervalTo: Instant? = null,

    @Column(name = "pickup_interval_from")
    var pickupIntervalFrom: Instant? = null,

    @Column(name = "pickup_interval_to")
    var pickupIntervalTo: Instant? = null,

    @Column(name = "created_at", nullable = false)
    var createdAt: Instant,

    @Column(name = "updated_at", nullable = false)
    var updatedAt: Instant,
)
