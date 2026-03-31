package ru.foodbox.delivery.modules.delivery.infrastructure.repository

import org.springframework.stereotype.Repository
import ru.foodbox.delivery.modules.delivery.domain.DeliveryMethodSetting
import ru.foodbox.delivery.modules.delivery.domain.DeliveryMethodType
import ru.foodbox.delivery.modules.delivery.domain.repository.DeliveryMethodSettingRepository
import ru.foodbox.delivery.modules.delivery.infrastructure.persistence.entity.DeliveryMethodSettingEntity
import ru.foodbox.delivery.modules.delivery.infrastructure.persistence.jpa.DeliveryMethodSettingJpaRepository
import java.time.Instant
import kotlin.jvm.optionals.getOrNull

@Repository
class DeliveryMethodSettingRepositoryImpl(
    private val jpaRepository: DeliveryMethodSettingJpaRepository,
) : DeliveryMethodSettingRepository {

    override fun findAll(): List<DeliveryMethodSetting> {
        return jpaRepository.findAllByOrderBySortOrderAscMethodAsc().map { it.toDomain() }
    }

    override fun findByMethod(method: DeliveryMethodType): DeliveryMethodSetting? {
        return jpaRepository.findById(method).getOrNull()?.toDomain()
    }

    override fun save(setting: DeliveryMethodSetting): DeliveryMethodSetting {
        val existing = jpaRepository.findById(setting.method).getOrNull()
        val now = Instant.now()
        val entity = existing ?: DeliveryMethodSettingEntity(
            method = setting.method,
            isEnabled = setting.enabled,
            sortOrder = setting.sortOrder,
            createdAt = now,
            updatedAt = now,
        )

        entity.isEnabled = setting.enabled
        entity.sortOrder = setting.sortOrder
        entity.updatedAt = now
        return jpaRepository.save(entity).toDomain()
    }

    private fun DeliveryMethodSettingEntity.toDomain(): DeliveryMethodSetting {
        return DeliveryMethodSetting(
            method = method,
            enabled = isEnabled,
            sortOrder = sortOrder,
        )
    }
}
