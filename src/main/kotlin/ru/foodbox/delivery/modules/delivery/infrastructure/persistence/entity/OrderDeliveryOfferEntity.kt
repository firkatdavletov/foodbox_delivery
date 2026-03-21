package ru.foodbox.delivery.modules.delivery.infrastructure.persistence.entity

import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.Id
import jakarta.persistence.Table
import java.time.Instant
import java.util.UUID

@Entity
@Table(name = "order_delivery_offers")
class OrderDeliveryOfferEntity(
    @Id
    @Column(nullable = false)
    var id: UUID,

    @Column(name = "order_id", nullable = false, unique = true)
    var orderId: UUID,

    @Column(name = "offer_id", nullable = false, unique = true)
    var offerId: UUID,

    @Column(name = "external_request_id", length = 128)
    var externalRequestId: String? = null,

    @Column(name = "confirmed_at")
    var confirmedAt: Instant? = null,

    @Column(name = "created_at", nullable = false)
    var createdAt: Instant,

    @Column(name = "updated_at", nullable = false)
    var updatedAt: Instant,
)
