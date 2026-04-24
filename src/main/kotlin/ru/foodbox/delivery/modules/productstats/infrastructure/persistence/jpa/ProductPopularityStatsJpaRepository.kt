package ru.foodbox.delivery.modules.productstats.infrastructure.persistence.jpa

import org.springframework.data.domain.Pageable
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Query
import ru.foodbox.delivery.modules.productstats.infrastructure.persistence.entity.ProductPopularityStatsEntity
import java.util.UUID

interface ProductPopularityStatsJpaRepository : JpaRepository<ProductPopularityStatsEntity, UUID> {
    fun findAllByProductIdIn(productIds: Collection<UUID>): List<ProductPopularityStatsEntity>

    @Query(
        """
            select s
            from ProductPopularityStatsEntity s, CatalogProductEntity p
            where p.id = s.productId
              and s.enabled = true
            order by s.manualScore desc, p.sortOrder asc, p.createdAt desc, s.productId asc
        """
    )
    fun findEnabledOrdered(): List<ProductPopularityStatsEntity>

    @Query(
        """
            select s.productId
            from ProductPopularityStatsEntity s, CatalogProductEntity p
            where p.id = s.productId
              and s.enabled = true
              and p.isActive = true
            order by s.manualScore desc, p.sortOrder asc, p.createdAt desc, s.productId asc
        """
    )
    fun findActivePopularProductIds(pageable: Pageable): List<UUID>
}
