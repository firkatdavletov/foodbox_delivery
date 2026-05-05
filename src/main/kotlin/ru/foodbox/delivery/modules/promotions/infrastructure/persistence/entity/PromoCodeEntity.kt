package ru.foodbox.delivery.modules.promotions.infrastructure.persistence.entity

import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.EnumType
import jakarta.persistence.Enumerated
import jakarta.persistence.Id
import jakarta.persistence.Index
import jakarta.persistence.Table
import ru.foodbox.delivery.modules.promotions.domain.PromoCodeDiscountType
import java.time.Instant
import java.util.UUID

@Entity
@Table(
    name = "promo_codes",
    indexes = [
        Index(name = "idx_promo_codes_code", columnList = "code"),
        Index(name = "idx_promo_codes_active", columnList = "active"),
    ],
)
class PromoCodeEntity(
    @Id
    @Column(nullable = false)
    var id: UUID,

    @Column(nullable = false, unique = true, length = 64)
    var code: String,

    @Enumerated(EnumType.STRING)
    @Column(name = "discount_type", nullable = false, length = 32)
    var discountType: PromoCodeDiscountType,

    @Column(name = "discount_value", nullable = false)
    var discountValue: Long,

    @Column(name = "min_order_amount_minor")
    var minOrderAmountMinor: Long? = null,

    @Column(name = "max_discount_minor")
    var maxDiscountMinor: Long? = null,

    @Column(length = 3)
    var currency: String? = null,

    @Column(name = "starts_at")
    var startsAt: Instant? = null,

    @Column(name = "ends_at")
    var endsAt: Instant? = null,

    @Column(name = "usage_limit_total")
    var usageLimitTotal: Int? = null,

    @Column(name = "usage_limit_per_user")
    var usageLimitPerUser: Int? = null,

    @Column(name = "used_count", nullable = false)
    var usedCount: Int = 0,

    @Column(nullable = false)
    var active: Boolean = true,

    @Column(name = "created_at", nullable = false)
    var createdAt: Instant,

    @Column(name = "updated_at", nullable = false)
    var updatedAt: Instant,
)
