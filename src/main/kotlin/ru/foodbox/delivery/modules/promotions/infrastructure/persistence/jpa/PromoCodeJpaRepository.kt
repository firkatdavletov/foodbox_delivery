package ru.foodbox.delivery.modules.promotions.infrastructure.persistence.jpa

import jakarta.persistence.LockModeType
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.JpaSpecificationExecutor
import org.springframework.data.jpa.repository.Lock
import org.springframework.data.jpa.repository.Query
import org.springframework.data.repository.query.Param
import ru.foodbox.delivery.modules.promotions.infrastructure.persistence.entity.PromoCodeEntity
import java.util.UUID

interface PromoCodeJpaRepository : JpaRepository<PromoCodeEntity, UUID>, JpaSpecificationExecutor<PromoCodeEntity> {

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select p from PromoCodeEntity p where upper(p.code) = upper(:code)")
    fun findByCodeForUpdate(@Param("code") code: String): PromoCodeEntity?

    @Query("select p from PromoCodeEntity p where upper(p.code) = upper(:code)")
    fun findByCodeIgnoreCase(@Param("code") code: String): PromoCodeEntity?
}
