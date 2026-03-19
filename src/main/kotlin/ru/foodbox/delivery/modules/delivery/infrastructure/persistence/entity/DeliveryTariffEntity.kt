package ru.foodbox.delivery.modules.delivery.infrastructure.persistence.entity

import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.EnumType
import jakarta.persistence.Enumerated
import jakarta.persistence.FetchType
import jakarta.persistence.Id
import jakarta.persistence.JoinColumn
import jakarta.persistence.ManyToOne
import jakarta.persistence.Table
import ru.foodbox.delivery.modules.delivery.domain.DeliveryMethodType
import java.time.Instant
import java.util.UUID

@Entity
@Table(name = "delivery_tariffs")
class DeliveryTariffEntity(
    @Id
    @Column(nullable = false)
    var id: UUID,

    @Enumerated(EnumType.STRING)
    @Column(name = "method", nullable = false, length = 32)
    var method: DeliveryMethodType,

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "zone_id")
    var zone: DeliveryZoneEntity? = null,

    @Column(name = "is_available", nullable = false)
    var isAvailable: Boolean,

    @Column(name = "fixed_price_minor", nullable = false)
    var fixedPriceMinor: Long,

    @Column(name = "free_from_amount_minor")
    var freeFromAmountMinor: Long? = null,

    @Column(name = "currency", nullable = false, length = 3)
    var currency: String,

    @Column(name = "estimated_days")
    var estimatedDays: Int? = null,

    @Column(name = "created_at", nullable = false)
    var createdAt: Instant,

    @Column(name = "updated_at", nullable = false)
    var updatedAt: Instant,
)
