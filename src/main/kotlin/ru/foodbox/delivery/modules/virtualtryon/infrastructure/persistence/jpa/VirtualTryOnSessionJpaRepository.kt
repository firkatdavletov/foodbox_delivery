package ru.foodbox.delivery.modules.virtualtryon.infrastructure.persistence.jpa

import org.springframework.data.jpa.repository.JpaRepository
import ru.foodbox.delivery.modules.virtualtryon.infrastructure.persistence.entity.VirtualTryOnSessionEntity
import java.util.UUID

interface VirtualTryOnSessionJpaRepository : JpaRepository<VirtualTryOnSessionEntity, UUID> {
    fun findByProviderPredictionId(providerPredictionId: String): VirtualTryOnSessionEntity?
}
