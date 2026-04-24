package ru.foodbox.delivery.modules.productstats.domain.repository

import ru.foodbox.delivery.modules.productstats.domain.ProductPopularityStats
import java.util.UUID

interface ProductPopularityStatsRepository {
    fun findByProductId(productId: UUID): ProductPopularityStats?
    fun findAllByProductIds(productIds: Collection<UUID>): List<ProductPopularityStats>
    fun findEnabled(): List<ProductPopularityStats>
    fun findPopularProductIds(limit: Int): List<UUID>
    fun save(stats: ProductPopularityStats): ProductPopularityStats
    fun saveAll(stats: Collection<ProductPopularityStats>): List<ProductPopularityStats>
}
