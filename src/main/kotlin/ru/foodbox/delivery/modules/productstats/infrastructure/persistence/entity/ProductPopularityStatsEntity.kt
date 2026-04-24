package ru.foodbox.delivery.modules.productstats.infrastructure.persistence.entity

import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.Id
import jakarta.persistence.Index
import jakarta.persistence.Table
import java.time.Instant
import java.util.UUID

@Entity
@Table(
    name = "product_popularity_stats",
    indexes = [
        Index(name = "idx_product_popularity_stats_enabled_score", columnList = "enabled, manual_score"),
    ],
)
class ProductPopularityStatsEntity(
    @Id
    @Column(name = "product_id", nullable = false)
    var productId: UUID,

    @Column(nullable = false)
    var enabled: Boolean = false,

    @Column(name = "manual_score", nullable = false)
    var manualScore: Int = 0,

    @Column(name = "updated_at", nullable = false)
    var updatedAt: Instant,
)
