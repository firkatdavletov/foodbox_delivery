package ru.foodbox.delivery.modules.delivery.domain

data class DeliveryMethodSetting(
    val method: DeliveryMethodType,
    val title: String,
    val description: String?,
    val isActive: Boolean,
    val sortOrder: Int,
) {
    companion object {
        fun defaultFor(method: DeliveryMethodType): DeliveryMethodSetting {
            return DeliveryMethodSetting(
                method = method,
                title = method.defaultTitle,
                description = method.defaultDescription,
                isActive = method.defaultIsActive,
                sortOrder = method.ordinal,
            )
        }
    }
}
