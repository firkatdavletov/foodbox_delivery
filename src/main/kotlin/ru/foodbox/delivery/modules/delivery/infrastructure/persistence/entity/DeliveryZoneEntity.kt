package ru.foodbox.delivery.modules.delivery.infrastructure.persistence.entity

import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.EnumType
import jakarta.persistence.Enumerated
import jakarta.persistence.Id
import jakarta.persistence.Table
import org.hibernate.annotations.JdbcTypeCode
import org.hibernate.type.SqlTypes
import org.locationtech.jts.geom.MultiPolygon
import ru.foodbox.delivery.modules.delivery.domain.DeliveryZoneType
import java.time.Instant
import java.util.UUID

@Entity
@Table(name = "delivery_zones")
class DeliveryZoneEntity(
    @Id
    @Column(nullable = false)
    var id: UUID,

    @Column(nullable = false, unique = true, length = 64)
    var code: String,

    @Column(nullable = false, length = 255)
    var name: String,

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 32)
    var type: DeliveryZoneType,

    @Column(length = 255)
    var city: String? = null,

    @Column(name = "normalized_city", length = 255)
    var normalizedCity: String? = null,

    @Column(name = "postal_code", length = 64)
    var postalCode: String? = null,

    @JdbcTypeCode(SqlTypes.GEOMETRY)
    @Column(name = "geometry")
    var geometry: MultiPolygon? = null,

    @Column(nullable = false)
    var priority: Int = 0,

    @Column(name = "is_active", nullable = false)
    var isActive: Boolean,

    @Column(name = "created_at", nullable = false)
    var createdAt: Instant,

    @Column(name = "updated_at", nullable = false)
    var updatedAt: Instant,
)
