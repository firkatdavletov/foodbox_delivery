package ru.foodbox.delivery.modules.delivery.domain

data class DeliveryMethodSetting(
    val method: DeliveryMethodType,
    val enabled: Boolean,
    val sortOrder: Int,
)
