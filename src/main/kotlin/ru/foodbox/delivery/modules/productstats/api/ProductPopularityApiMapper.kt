package ru.foodbox.delivery.modules.productstats.api

import ru.foodbox.delivery.modules.catalog.api.toResponse
import ru.foodbox.delivery.modules.catalog.domain.CatalogProduct
import ru.foodbox.delivery.modules.productstats.api.dto.ProductPopularityAdminItemResponse
import ru.foodbox.delivery.modules.productstats.api.dto.ProductPopularityStatsResponse
import ru.foodbox.delivery.modules.productstats.domain.ProductPopularityStats
import java.util.UUID

internal fun ProductPopularityStats?.toResponse(productId: UUID): ProductPopularityStatsResponse {
    return ProductPopularityStatsResponse(
        productId = productId,
        enabled = this?.enabled ?: false,
        manualScore = this?.manualScore ?: 0,
        updatedAt = this?.updatedAt,
    )
}

internal fun ProductPopularityStats.toAdminItemResponse(product: CatalogProduct): ProductPopularityAdminItemResponse {
    return ProductPopularityAdminItemResponse(
        product = product.toResponse(),
        enabled = enabled,
        manualScore = manualScore,
        updatedAt = updatedAt,
    )
}
