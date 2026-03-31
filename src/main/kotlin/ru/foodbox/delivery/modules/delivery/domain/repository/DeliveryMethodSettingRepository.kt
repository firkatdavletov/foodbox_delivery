package ru.foodbox.delivery.modules.delivery.domain.repository

import ru.foodbox.delivery.modules.delivery.domain.DeliveryMethodSetting
import ru.foodbox.delivery.modules.delivery.domain.DeliveryMethodType

interface DeliveryMethodSettingRepository {
    fun findAll(): List<DeliveryMethodSetting>
    fun findByMethod(method: DeliveryMethodType): DeliveryMethodSetting?
    fun save(setting: DeliveryMethodSetting): DeliveryMethodSetting
}
