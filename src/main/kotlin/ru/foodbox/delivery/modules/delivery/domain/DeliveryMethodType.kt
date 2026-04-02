package ru.foodbox.delivery.modules.delivery.domain

import com.fasterxml.jackson.annotation.JsonCreator

enum class DeliveryMethodType(
    val apiCode: String,
    val displayName: String,
    val requiresAddress: Boolean,
    val requiresPickupPoint: Boolean,
    val isActive: Boolean,
) {
    PICKUP(
        apiCode = "pickup",
        displayName = "Самовывоз",
        requiresAddress = false,
        requiresPickupPoint = true,
        isActive = false,
    ),
    COURIER(
        apiCode = "courier",
        displayName = "Доставка",
        requiresAddress = true,
        requiresPickupPoint = false,
        isActive = false,
    ),
    YANDEX_PICKUP_POINT(
        apiCode = "yandex_pickup_point",
        displayName = "Доставка в ПВЗ Яндекс Маркет",
        requiresAddress = false,
        requiresPickupPoint = true,
        isActive = true,
    ),

    ;

    companion object {
        @JvmStatic
        @JsonCreator
        fun fromValue(value: String): DeliveryMethodType {
            return entries.firstOrNull {
                it.name.equals(value, ignoreCase = true) || it.apiCode.equals(value, ignoreCase = true)
            } ?: throw IllegalArgumentException("Unsupported delivery method: $value")
        }
    }
}
