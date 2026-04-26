package ru.foodbox.delivery.modules.admin.auth.infrastructure.persistence.jpa

import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Modifying
import org.springframework.data.jpa.repository.Query
import org.springframework.data.repository.query.Param
import ru.foodbox.delivery.modules.admin.auth.infrastructure.persistence.entity.AdminAuthSessionEntity
import java.time.Instant
import java.util.UUID

interface AdminAuthSessionJpaRepository : JpaRepository<AdminAuthSessionEntity, UUID> {
    fun findByRefreshTokenHash(refreshTokenHash: String): AdminAuthSessionEntity?

    @Modifying
    @Query(
        """
        update AdminAuthSessionEntity s
           set s.revokedAt = :revokedAt
         where s.adminId = :adminId
           and s.revokedAt is null
        """
    )
    fun revokeAllByAdminId(
        @Param("adminId") adminId: UUID,
        @Param("revokedAt") revokedAt: Instant,
    ): Int
}
