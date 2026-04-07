package ru.foodbox.delivery.modules.herobanners.infrastructure.persistence.jpa

import org.springframework.data.domain.Page
import org.springframework.data.domain.Pageable
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Query
import org.springframework.data.repository.query.Param
import ru.foodbox.delivery.modules.herobanners.domain.BannerPlacement
import ru.foodbox.delivery.modules.herobanners.domain.BannerStatus
import ru.foodbox.delivery.modules.herobanners.infrastructure.persistence.entity.HeroBannerEntity
import java.time.Instant
import java.util.UUID

interface HeroBannerJpaRepository : JpaRepository<HeroBannerEntity, UUID> {

    @Query(
        """
        SELECT b FROM HeroBannerEntity b
        WHERE b.deletedAt IS NULL
          AND b.status = :status
          AND b.storefrontCode = :storefrontCode
          AND b.placement = :placement
          AND (b.startsAt IS NULL OR b.startsAt <= :now)
          AND (b.endsAt IS NULL OR b.endsAt > :now)
        ORDER BY b.sortOrder ASC, b.createdAt ASC
        """
    )
    fun findActiveForStorefront(
        @Param("storefrontCode") storefrontCode: String,
        @Param("placement") placement: BannerPlacement,
        @Param("status") status: BannerStatus,
        @Param("now") now: Instant,
    ): List<HeroBannerEntity>

    @Query(
        value = """
        SELECT DISTINCT b FROM HeroBannerEntity b
        WHERE b.deletedAt IS NULL
          AND (:storefrontCode IS NULL OR b.storefrontCode = :storefrontCode)
          AND (:placement IS NULL OR b.placement = :placement)
          AND (:status IS NULL OR b.status = :status)
        ORDER BY b.sortOrder ASC, b.createdAt ASC
    """,
        countQuery = """
        SELECT COUNT(b.id) FROM HeroBannerEntity b
        WHERE b.deletedAt IS NULL
          AND (:storefrontCode IS NULL OR b.storefrontCode = :storefrontCode)
          AND (:placement IS NULL OR b.placement = :placement)
          AND (:status IS NULL OR b.status = :status)
    """
    )
    fun findAllAdminWithoutSearch(
        @Param("storefrontCode") storefrontCode: String?,
        @Param("placement") placement: BannerPlacement?,
        @Param("status") status: BannerStatus?,
        pageable: Pageable,
    ): Page<HeroBannerEntity>

    @Query(
        value = """
        SELECT DISTINCT b FROM HeroBannerEntity b
        LEFT JOIN b.translations t
        WHERE b.deletedAt IS NULL
          AND (:storefrontCode IS NULL OR b.storefrontCode = :storefrontCode)
          AND (:placement IS NULL OR b.placement = :placement)
          AND (:status IS NULL OR b.status = :status)
          AND (
              LOWER(b.code) LIKE :searchPattern
              OR LOWER(t.title) LIKE :searchPattern
          )
        ORDER BY b.sortOrder ASC, b.createdAt ASC
    """,
        countQuery = """
        SELECT COUNT(DISTINCT b.id) FROM HeroBannerEntity b
        LEFT JOIN b.translations t
        WHERE b.deletedAt IS NULL
          AND (:storefrontCode IS NULL OR b.storefrontCode = :storefrontCode)
          AND (:placement IS NULL OR b.placement = :placement)
          AND (:status IS NULL OR b.status = :status)
          AND (
              LOWER(b.code) LIKE :searchPattern
              OR LOWER(t.title) LIKE :searchPattern
          )
    """
    )
    fun findAllAdminWithSearch(
        @Param("storefrontCode") storefrontCode: String?,
        @Param("placement") placement: BannerPlacement?,
        @Param("status") status: BannerStatus?,
        @Param("searchPattern") searchPattern: String,
        pageable: Pageable,
    ): Page<HeroBannerEntity>

    fun findAllByIdIn(ids: Collection<UUID>): List<HeroBannerEntity>
}
