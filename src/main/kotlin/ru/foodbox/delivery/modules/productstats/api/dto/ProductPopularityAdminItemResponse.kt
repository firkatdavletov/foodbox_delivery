package ru.foodbox.delivery.modules.productstats.api.dto

import ru.foodbox.delivery.modules.catalog.api.dto.ProductResponse
import java.time.Instant

data class ProductPopularityAdminItemResponse(
    val product: ProductResponse,
    val enabled: Boolean,
    val manualScore: Int,
    val updatedAt: Instant,
)
