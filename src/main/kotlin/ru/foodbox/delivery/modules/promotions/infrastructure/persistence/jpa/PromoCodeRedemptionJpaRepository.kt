package ru.foodbox.delivery.modules.promotions.infrastructure.persistence.jpa

import org.springframework.data.jpa.repository.JpaRepository
import ru.foodbox.delivery.modules.promotions.infrastructure.persistence.entity.PromoCodeRedemptionEntity
import java.util.UUID

interface PromoCodeRedemptionJpaRepository : JpaRepository<PromoCodeRedemptionEntity, UUID> {
    fun countByPromoCodeIdAndUserId(promoCodeId: UUID, userId: UUID): Long
}
