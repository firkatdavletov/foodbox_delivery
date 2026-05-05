package ru.foodbox.delivery.modules.promotions.infrastructure.persistence.entity

import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.Id
import jakarta.persistence.Index
import jakarta.persistence.Table
import java.time.Instant
import java.util.UUID

@Entity
@Table(
    name = "promo_code_redemptions",
    indexes = [
        Index(name = "idx_promo_code_redemptions_promo_code_id", columnList = "promo_code_id"),
        Index(name = "idx_promo_code_redemptions_user_id", columnList = "user_id"),
        Index(name = "idx_promo_code_redemptions_order_id", columnList = "order_id"),
    ],
)
class PromoCodeRedemptionEntity(
    @Id
    @Column(nullable = false)
    var id: UUID,

    @Column(name = "promo_code_id", nullable = false)
    var promoCodeId: UUID,

    @Column(name = "order_id", nullable = false, unique = true)
    var orderId: UUID,

    @Column(name = "user_id")
    var userId: UUID? = null,

    @Column(name = "discount_minor", nullable = false)
    var discountMinor: Long,

    @Column(name = "created_at", nullable = false)
    var createdAt: Instant,
)
