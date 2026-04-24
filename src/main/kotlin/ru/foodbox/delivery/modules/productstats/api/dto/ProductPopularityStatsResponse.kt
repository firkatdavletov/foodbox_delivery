package ru.foodbox.delivery.modules.productstats.api.dto

import java.time.Instant
import java.util.UUID

data class ProductPopularityStatsResponse(
    val productId: UUID,
    val enabled: Boolean,
    val manualScore: Int,
    val updatedAt: Instant?,
)
