package ru.foodbox.delivery.modules.delivery.domain

import com.fasterxml.jackson.annotation.JsonCreator

enum class DeliveryMethodType(
    val apiCode: String,
    val defaultTitle: String,
    val defaultDescription: String?,
    val requiresAddress: Boolean,
    val requiresPickupPoint: Boolean,
    val defaultIsActive: Boolean,
) {
    PICKUP(
        apiCode = "pickup",
        defaultTitle = "Самовывоз",
        defaultDescription = "Заберите заказ в пункте самовывоза",
        requiresAddress = false,
        requiresPickupPoint = true,
        defaultIsActive = false,
    ),
    COURIER(
        apiCode = "courier",
        defaultTitle = "Доставка",
        defaultDescription = "Курьер доставит заказ по указанному адресу",
        requiresAddress = true,
        requiresPickupPoint = false,
        defaultIsActive = false,
    ),
    YANDEX_PICKUP_POINT(
        apiCode = "yandex_pickup_point",
        defaultTitle = "Доставка в ПВЗ Яндекс Маркет",
        defaultDescription = "Получение заказа в пункте выдачи Яндекс Маркета",
        requiresAddress = false,
        requiresPickupPoint = true,
        defaultIsActive = true,
    ),
    CUSTOM_DELIVERY_ADDRESS(
        apiCode = "custom_delivery_address",
        defaultTitle = "Доставка по согласованию",
        defaultDescription = "Адрес и условия доставки согласовываются отдельно после оформления заказа",
        requiresAddress = true,
        requiresPickupPoint = false,
        defaultIsActive = false,
    ),
    ;

    val displayName: String
        get() = defaultTitle

    val isActive: Boolean
        get() = defaultIsActive

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
