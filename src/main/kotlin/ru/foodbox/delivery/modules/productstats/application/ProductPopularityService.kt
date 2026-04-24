package ru.foodbox.delivery.modules.productstats.application

import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import ru.foodbox.delivery.common.error.NotFoundException
import ru.foodbox.delivery.modules.catalog.domain.repository.CatalogProductRepository
import ru.foodbox.delivery.modules.productstats.application.command.ReorderProductPopularityCommand
import ru.foodbox.delivery.modules.productstats.application.command.UpsertProductPopularityCommand
import ru.foodbox.delivery.modules.productstats.domain.ProductPopularityStats
import ru.foodbox.delivery.modules.productstats.domain.repository.ProductPopularityStatsRepository
import java.time.Instant
import java.util.UUID

@Service
class ProductPopularityService(
    private val statsRepository: ProductPopularityStatsRepository,
    private val productRepository: CatalogProductRepository,
) {

    @Transactional(readOnly = true)
    fun findPopularProductIds(limit: Int): List<UUID> {
        require(limit in 1..MAX_POPULAR_PRODUCTS_LIMIT) {
            "limit must be between 1 and $MAX_POPULAR_PRODUCTS_LIMIT"
        }
        return statsRepository.findPopularProductIds(limit)
    }

    @Transactional(readOnly = true)
    fun getEnabledStats(): List<ProductPopularityStats> {
        return statsRepository.findEnabled()
    }

    @Transactional(readOnly = true)
    fun getStats(productId: UUID): ProductPopularityStats? {
        ensureProductExists(productId)
        return statsRepository.findByProductId(productId)
    }

    @Transactional
    fun upsertStats(productId: UUID, command: UpsertProductPopularityCommand): ProductPopularityStats {
        require(command.manualScore >= 0) { "manualScore must be greater than or equal to zero" }
        ensureProductExists(productId)

        val now = Instant.now()
        val stats = statsRepository.findByProductId(productId)?.copy(
            enabled = command.enabled,
            manualScore = command.manualScore,
            updatedAt = now,
        ) ?: ProductPopularityStats(
            productId = productId,
            enabled = command.enabled,
            manualScore = command.manualScore,
            updatedAt = now,
        )

        return statsRepository.save(stats)
    }

    @Transactional
    fun reorder(command: ReorderProductPopularityCommand): List<ProductPopularityStats> {
        require(command.productIds.size <= MAX_POPULAR_PRODUCTS_LIMIT) {
            "productIds size must be less than or equal to $MAX_POPULAR_PRODUCTS_LIMIT"
        }
        require(command.productIds.toSet().size == command.productIds.size) {
            "productIds must not contain duplicates"
        }

        val productsById = productRepository.findAllByIds(command.productIds).associateBy { it.id }
        val missingProductId = command.productIds.firstOrNull { it !in productsById }
        if (missingProductId != null) {
            throw NotFoundException("Product not found")
        }

        val now = Instant.now()
        val productIds = command.productIds.toSet()
        val existingStatsByProductId = statsRepository.findAllByProductIds(command.productIds).associateBy { it.productId }
        val disabledStats = statsRepository.findEnabled()
            .filterNot { it.productId in productIds }
            .map {
                it.copy(
                    enabled = false,
                    updatedAt = now,
                )
            }
        val reorderedStats = command.productIds.mapIndexed { index, productId ->
            val manualScore = scoreForPosition(index)
            existingStatsByProductId[productId]?.copy(
                enabled = true,
                manualScore = manualScore,
                updatedAt = now,
            ) ?: ProductPopularityStats(
                productId = productId,
                enabled = true,
                manualScore = manualScore,
                updatedAt = now,
            )
        }

        statsRepository.saveAll(disabledStats + reorderedStats)
        return reorderedStats
    }

    private fun ensureProductExists(productId: UUID) {
        productRepository.findById(productId)
            ?: throw NotFoundException("Product not found")
    }

    private fun scoreForPosition(index: Int): Int {
        return POPULARITY_REORDER_BASE_SCORE - index * POPULARITY_REORDER_SCORE_STEP
    }

    companion object {
        const val MAX_POPULAR_PRODUCTS_LIMIT = 100
        private const val POPULARITY_REORDER_BASE_SCORE = 10_000
        private const val POPULARITY_REORDER_SCORE_STEP = 10
    }
}
