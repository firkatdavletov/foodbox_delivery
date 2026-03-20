package ru.foodbox.delivery.modules.payments.domain

import com.fasterxml.jackson.annotation.JsonCreator

enum class PaymentMethodCode(
    val apiCode: String,
    val displayName: String,
    val description: String?,
    val isOnline: Boolean,
) {
    CASH(
        apiCode = "cash",
        displayName = "Наличными при получении",
        description = "Оплата наличными при получении заказа",
        isOnline = false,
    ),
    CARD_ON_DELIVERY(
        apiCode = "card_on_delivery",
        displayName = "Картой при получении",
        description = "Оплата банковской картой при получении заказа",
        isOnline = false,
    ),
    CARD_ONLINE(
        apiCode = "card_online",
        displayName = "Онлайн картой",
        description = "Оплата банковской картой онлайн",
        isOnline = true,
    ),
    SBP(
        apiCode = "sbp",
        displayName = "СБП",
        description = "Оплата через Систему быстрых платежей",
        isOnline = true,
    ),
    ;

    companion object {
        @JvmStatic
        @JsonCreator
        fun fromValue(value: String): PaymentMethodCode {
            return entries.firstOrNull {
                it.name.equals(value, ignoreCase = true) || it.apiCode.equals(value, ignoreCase = true)
            } ?: throw IllegalArgumentException("Unsupported payment method: $value")
        }
    }
}
