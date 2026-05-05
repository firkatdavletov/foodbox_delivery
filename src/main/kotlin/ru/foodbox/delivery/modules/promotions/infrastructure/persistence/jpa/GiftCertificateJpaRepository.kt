package ru.foodbox.delivery.modules.promotions.infrastructure.persistence.jpa

import jakarta.persistence.LockModeType
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Lock
import org.springframework.data.jpa.repository.Query
import org.springframework.data.repository.query.Param
import ru.foodbox.delivery.modules.promotions.infrastructure.persistence.entity.GiftCertificateEntity
import java.util.UUID

interface GiftCertificateJpaRepository : JpaRepository<GiftCertificateEntity, UUID> {

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select g from GiftCertificateEntity g where upper(g.code) = upper(:code)")
    fun findByCodeForUpdate(@Param("code") code: String): GiftCertificateEntity?
}
