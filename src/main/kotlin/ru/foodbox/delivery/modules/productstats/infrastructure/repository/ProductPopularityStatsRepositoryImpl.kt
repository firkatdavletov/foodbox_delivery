package ru.foodbox.delivery.modules.productstats.infrastructure.repository

import org.springframework.data.domain.PageRequest
import org.springframework.stereotype.Repository
import ru.foodbox.delivery.modules.productstats.domain.ProductPopularityStats
import ru.foodbox.delivery.modules.productstats.domain.repository.ProductPopularityStatsRepository
import ru.foodbox.delivery.modules.productstats.infrastructure.persistence.entity.ProductPopularityStatsEntity
import ru.foodbox.delivery.modules.productstats.infrastructure.persistence.jpa.ProductPopularityStatsJpaRepository
import java.util.UUID
import kotlin.jvm.optionals.getOrNull

@Repository
class ProductPopularityStatsRepositoryImpl(
    private val jpaRepository: ProductPopularityStatsJpaRepository,
) : ProductPopularityStatsRepository {

    override fun findByProductId(productId: UUID): ProductPopularityStats? {
        return jpaRepository.findById(productId).getOrNull()?.let(::toDomain)
    }

    override fun findAllByProductIds(productIds: Collection<UUID>): List<ProductPopularityStats> {
        if (productIds.isEmpty()) {
            return emptyList()
        }
        return jpaRepository.findAllByProductIdIn(productIds).map(::toDomain)
    }

    override fun findEnabled(): List<ProductPopularityStats> {
        return jpaRepository.findEnabledOrdered().map(::toDomain)
    }

    override fun findPopularProductIds(limit: Int): List<UUID> {
        return jpaRepository.findActivePopularProductIds(PageRequest.of(0, limit))
    }

    override fun save(stats: ProductPopularityStats): ProductPopularityStats {
        val existing = jpaRepository.findById(stats.productId).getOrNull()
        val entity = existing ?: ProductPopularityStatsEntity(
            productId = stats.productId,
            enabled = stats.enabled,
            manualScore = stats.manualScore,
            updatedAt = stats.updatedAt,
        )

        entity.enabled = stats.enabled
        entity.manualScore = stats.manualScore
        entity.updatedAt = stats.updatedAt

        return toDomain(jpaRepository.save(entity))
    }

    override fun saveAll(stats: Collection<ProductPopularityStats>): List<ProductPopularityStats> {
        if (stats.isEmpty()) {
            return emptyList()
        }
        return jpaRepository.saveAll(stats.map(::toEntity)).map(::toDomain)
    }

    private fun toEntity(stats: ProductPopularityStats): ProductPopularityStatsEntity {
        return ProductPopularityStatsEntity(
            productId = stats.productId,
            enabled = stats.enabled,
            manualScore = stats.manualScore,
            updatedAt = stats.updatedAt,
        )
    }

    private fun toDomain(entity: ProductPopularityStatsEntity): ProductPopularityStats {
        return ProductPopularityStats(
            productId = entity.productId,
            enabled = entity.enabled,
            manualScore = entity.manualScore,
            updatedAt = entity.updatedAt,
        )
    }
}
