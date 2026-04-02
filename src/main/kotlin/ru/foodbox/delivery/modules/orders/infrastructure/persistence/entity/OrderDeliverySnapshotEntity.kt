package ru.foodbox.delivery.modules.orders.infrastructure.persistence.entity

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
import java.util.UUID

@Entity
@Table(name = "order_delivery_snapshots")
class OrderDeliverySnapshotEntity(
    @Id
    @Column(nullable = false)
    var id: UUID,

    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "order_id", nullable = false, unique = true)
    var order: OrderEntity,

    @Enumerated(EnumType.STRING)
    @Column(name = "delivery_method", nullable = false, length = 32)
    var method: DeliveryMethodType,

    @Column(name = "delivery_method_name", nullable = false, length = 64)
    var methodName: String,

    @Column(name = "price_minor", nullable = false)
    var priceMinor: Long,

    @Column(name = "currency", nullable = false, length = 3)
    var currency: String,

    @Column(name = "zone_code", length = 64)
    var zoneCode: String? = null,

    @Column(name = "zone_name", length = 255)
    var zoneName: String? = null,

    @Column(name = "estimated_days")
    var estimatedDays: Int? = null,

    @Column(name = "estimates_minutes")
    var estimatesMinutes: Int? = null,

    @Column(name = "pickup_point_id")
    var pickupPointId: UUID? = null,

    @Column(name = "pickup_point_external_id", length = 64)
    var pickupPointExternalId: String? = null,

    @Column(name = "pickup_point_name", length = 255)
    var pickupPointName: String? = null,

    @Column(name = "pickup_point_address", length = 500)
    var pickupPointAddress: String? = null,

    @Embedded
    var address: DeliveryAddressEmbeddable? = null,
)
