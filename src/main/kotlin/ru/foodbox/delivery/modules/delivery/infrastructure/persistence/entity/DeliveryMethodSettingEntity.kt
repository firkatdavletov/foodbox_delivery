package ru.foodbox.delivery.modules.delivery.infrastructure.persistence.entity

import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.EnumType
import jakarta.persistence.Enumerated
import jakarta.persistence.Id
import jakarta.persistence.Table
import ru.foodbox.delivery.modules.delivery.domain.DeliveryMethodType
import java.time.Instant

@Entity
@Table(name = "delivery_method_settings")
class DeliveryMethodSettingEntity(
    @Id
    @Enumerated(EnumType.STRING)
    @Column(name = "method", nullable = false, length = 32)
    var method: DeliveryMethodType,

    @Column(name = "is_enabled", nullable = false)
    var isEnabled: Boolean,

    @Column(name = "sort_order", nullable = false)
    var sortOrder: Int,

    @Column(name = "created_at", nullable = false)
    var createdAt: Instant,

    @Column(name = "updated_at", nullable = false)
    var updatedAt: Instant,
)
