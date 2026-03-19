package ru.foodbox.delivery.modules.cart.infrastructure.persistence.entity

import jakarta.persistence.Column
import jakarta.persistence.Embedded
import jakarta.persistence.Entity
import jakarta.persistence.EnumType
import jakarta.persistence.Enumerated
import jakarta.persistence.FetchType
import jakarta.persistence.Id
import jakarta.persistence.JoinColumn
import jakarta.persistence.OneToOne
import jakarta.persistence.Table
import ru.foodbox.delivery.modules.delivery.domain.DeliveryMethodType
import ru.foodbox.delivery.modules.delivery.infrastructure.persistence.embedded.DeliveryAddressEmbeddable
import java.time.Instant
import java.util.UUID

@Entity
@Table(name = "cart_delivery_drafts")
class CartDeliveryDraftEntity(
    @Id
    @Column(nullable = false)
    var id: UUID,

    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "cart_id", nullable = false, unique = true)
    var cart: CartEntity,

    @Enumerated(EnumType.STRING)
    @Column(name = "delivery_method", nullable = false, length = 32)
    var deliveryMethod: DeliveryMethodType,

    @Embedded
    var deliveryAddress: DeliveryAddressEmbeddable? = null,

    @Column(name = "pickup_point_id")
    var pickupPointId: UUID? = null,

    @Column(name = "pickup_point_external_id", length = 64)
    var pickupPointExternalId: String? = null,

    @Column(name = "pickup_point_name", length = 255)
    var pickupPointName: String? = null,

    @Column(name = "pickup_point_address", length = 500)
    var pickupPointAddress: String? = null,

    @Column(name = "quote_available")
    var quoteAvailable: Boolean? = null,

    @Column(name = "quote_price_minor")
    var quotePriceMinor: Long? = null,

    @Column(name = "quote_currency", length = 3)
    var quoteCurrency: String? = null,

    @Column(name = "quote_zone_code", length = 64)
    var quoteZoneCode: String? = null,

    @Column(name = "quote_zone_name", length = 255)
    var quoteZoneName: String? = null,

    @Column(name = "quote_estimated_days")
    var quoteEstimatedDays: Int? = null,

    @Column(name = "quote_message", length = 500)
    var quoteMessage: String? = null,

    @Column(name = "quote_calculated_at")
    var quoteCalculatedAt: Instant? = null,

    @Column(name = "quote_expires_at")
    var quoteExpiresAt: Instant? = null,

    @Column(name = "created_at", nullable = false)
    var createdAt: Instant,

    @Column(name = "updated_at", nullable = false)
    var updatedAt: Instant,
)
