package ru.foodbox.delivery.utils

import ru.foodbox.delivery.data.DeliveryType
import ru.foodbox.delivery.data.entities.OrderStatus
import ru.foodbox.delivery.services.dto.OrderDto
import ru.foodbox.delivery.services.dto.UserDto

object OrderUtility {
    fun createOrderInfo(order: OrderDto, user: UserDto): String {
        val info = buildString {
            append("Заказ №${order.id}")
            append("\n")
            append("\n")
            append("Гость:")
            append("\n")
            append("Имя: ${user.name}")
            append("\n")
            append("Номер телефона: +${user.phone}")
            append("\n")
            append("\n")
            append("Состав заказа:")
            append("\n")

            for (item in order.items) {
                append("${item.name} - ${item.quantity}\n")
            }

            append("\n")
            append("Тип доставки: ${if (order.deliveryType == DeliveryType.DELIVERY) "Доставка" else "Самовывоз"}")
            append("\n")
            append("Адрес: ${order.deliveryAddress}")
            append("\n")
            append("Комментарий: ${order.comment}")
            append("\n")
            append("\n")
            append("Стоимость доставки: ${order.deliveryPrice.toInt()} ₽")
            append("\n")
            append("Сумма к оплате: ${order.totalAmount.toInt()} ₽")
            append("\n")
            append("\n")
            append("Статус: ${OrderStatus.getStatusName(order.status)}")
        }
        return info
    }
}