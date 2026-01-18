package ru.foodbox.delivery.data.entities

import ru.foodbox.delivery.data.DeliveryType

enum class OrderStatus {
    PENDING,
    AWAITING_PAYMENT,
    PAID,
    PROCESSING,
    AWAITING_COURIER,
    AWAITING_RECEIPT,
    DELIVERY,
    FAILED,
    CANCELLED,
    COMPLETED;

    companion object {
        fun nextStatus(currentStatus: OrderStatus, deliveryType: DeliveryType, paymentType: PaymentType): OrderStatus {
            return when (currentStatus) {
                PENDING -> when (paymentType) {
                    PaymentType.CASH -> PROCESSING
                    PaymentType.ONLINE -> AWAITING_PAYMENT
                }
                AWAITING_PAYMENT -> PAID
                PAID -> PROCESSING
                PROCESSING -> when (deliveryType) {
                    DeliveryType.DELIVERY -> AWAITING_COURIER
                    DeliveryType.PICKUP -> AWAITING_RECEIPT
                }
                AWAITING_COURIER -> DELIVERY
                AWAITING_RECEIPT -> COMPLETED
                DELIVERY -> COMPLETED
                FAILED -> FAILED
                CANCELLED -> CANCELLED
                COMPLETED -> COMPLETED
            }
        }

        fun getNextButtonText(currentStatus: OrderStatus, deliveryType: DeliveryType, paymentType: PaymentType): String {
            return when (currentStatus) {
                PENDING -> "Подтвердить заказ"
                AWAITING_PAYMENT -> "Заказ оплачен"
                PAID -> "Передан на кухню"
                PROCESSING -> "Заказ готов"
                AWAITING_COURIER -> "Курьер забрал заказ"
                DELIVERY -> "Заказ доставлен"
                AWAITING_RECEIPT -> "Заказ получен"
                FAILED -> "Заказ отменен по ошибке"
                CANCELLED -> "Заказ отменен"
                COMPLETED -> "Заказ выполнен"
            }
        }

        fun getStatusName(currentStatus: OrderStatus): String {
            return when (currentStatus) {
                PENDING -> "Ожидает подтверждения"
                AWAITING_PAYMENT -> "Ожидает оплаты"
                PAID -> "Оплачен"
                PROCESSING -> "Готовится"
                AWAITING_COURIER -> "Ожидает курьера"
                AWAITING_RECEIPT -> "Ожидает получения"
                DELIVERY -> "В доставке"
                FAILED -> "Ошибка"
                CANCELLED -> "Заказ отменен"
                COMPLETED -> "Заказ выполнен"
            }
        }
    }
}
