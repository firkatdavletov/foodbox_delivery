package ru.foodbox.delivery.modules.delivery.infrastructure.persistence.jpa

import org.springframework.data.jpa.repository.JpaRepository
import ru.foodbox.delivery.modules.delivery.infrastructure.persistence.entity.PickupPointEntity
import java.util.UUID

interface PickupPointJpaRepository : JpaRepository<PickupPointEntity, UUID> {
    fun findAllByIsActiveTrueOrderByNameAsc(): List<PickupPointEntity>
}
