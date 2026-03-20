package ru.foodbox.delivery.modules.payments.domain

enum class PaymentStatus {
    AWAITING_PAYMENT,
    PENDING,
    SUCCEEDED,
    FAILED,
    CANCELED,
    ;

    fun isTerminal(): Boolean {
        return this == SUCCEEDED || this == FAILED || this == CANCELED
    }
}
