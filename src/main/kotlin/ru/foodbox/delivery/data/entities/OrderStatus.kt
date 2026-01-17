package ru.foodbox.delivery.data.entities

enum class OrderStatus {
    PENDING, AWAITING_PAYMENT, AWAITING_CASH_PAYMENT, PAID, PROCESSING, FAILED, CANCELLED, DELIVERED;

    companion object {
        fun nextStatus(currentStatus: OrderStatus): OrderStatus {
            return when (currentStatus) {
                PENDING -> {
                    PAID
                }
                AWAITING_PAYMENT -> {
                    PROCESSING
                }
                AWAITING_CASH_PAYMENT -> {
                    PROCESSING
                }
                PAID -> {
                    PROCESSING
                }
                PROCESSING -> {
                    DELIVERED
                }
                FAILED -> {
                    FAILED
                }
                CANCELLED -> {
                    CANCELLED
                }
                DELIVERED -> {
                    DELIVERED
                }
            }
        }

        fun getNextButtonText(currentStatus: OrderStatus): String {
            return when (currentStatus) {
                PENDING -> {
                    "Подтвердить заказ"
                }
                AWAITING_PAYMENT -> "Заказ оплачен"
                AWAITING_CASH_PAYMENT -> ""
                PAID -> "Заказ передан курьеру"
                PROCESSING -> "Заказ доставлен"
                FAILED -> ""
                CANCELLED -> "Заказ отменен"
                DELIVERED -> "Заказ доставлен"
            }
        }

        fun getStatusName(currentStatus: OrderStatus): String {
            return when (currentStatus) {
                PENDING -> {
                    "Ожидание"
                }
                AWAITING_PAYMENT -> ""
                AWAITING_CASH_PAYMENT -> ""
                PAID -> "Готовится"
                PROCESSING -> "В доставке"
                FAILED -> ""
                CANCELLED -> "Заказ отменен"
                DELIVERED -> "Заказ доставлен"
            }
        }
    }
}
