package ru.foodbox.delivery.modules.delivery.infrastructure.persistence.jpa

import org.springframework.data.jpa.repository.JpaRepository
import ru.foodbox.delivery.modules.delivery.domain.DeliveryMethodType
import ru.foodbox.delivery.modules.delivery.infrastructure.persistence.entity.DeliveryMethodSettingEntity

interface DeliveryMethodSettingJpaRepository : JpaRepository<DeliveryMethodSettingEntity, DeliveryMethodType> {
    fun findAllByOrderBySortOrderAscMethodAsc(): List<DeliveryMethodSettingEntity>
}
