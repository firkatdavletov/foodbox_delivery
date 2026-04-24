package ru.foodbox.delivery.modules.productstats.domain

import java.time.Instant
import java.util.UUID

data class ProductPopularityStats(
    val productId: UUID,
    val enabled: Boolean,
    val manualScore: Int,
    val updatedAt: Instant,
)
