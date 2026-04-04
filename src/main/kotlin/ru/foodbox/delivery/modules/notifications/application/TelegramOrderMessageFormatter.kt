package ru.foodbox.delivery.modules.notifications.application

import org.springframework.stereotype.Component
import ru.foodbox.delivery.modules.orders.domain.Order
import ru.foodbox.delivery.modules.orders.domain.OrderStatusDefinition
import java.math.BigDecimal
import java.math.RoundingMode

@Component
class TelegramOrderMessageFormatter {

    fun formatOrderCreated(order: Order): String {
        return buildString {
            appendLine("Получен новый заказ")
            appendLine("Номер: ${order.orderNumber}")
            appendLine("Статус: ${order.currentStatus.name} (${order.currentStatus.code})")
            appendLine("Способ доставки: ${order.delivery.methodName}")
            appendLine("Сумма: ${formatMoney(order.totalMinor)}")
            appendLine("Покупатель: ${order.customerName ?: "N/A"}")
            appendLine("Телефон: ${order.customerPhone ?: "N/A"}")
            appendLine("Адрес: ${deliveryAddress(order)}")
            appendLine("Товары:")
            append(itemsBlock(order))
            appendLine()
            append("Время создания: ${order.createdAt}")
        }
    }

    fun formatOrderStatusChanged(order: Order, previousStatus: OrderStatusDefinition): String {
        return buildString {
            appendLine("Статус заказа изменен")
            appendLine("Номер: ${order.orderNumber}")
            appendLine("Статус: ${previousStatus.name} (${previousStatus.code}) -> ${order.currentStatus.name} (${order.currentStatus.code})")
            appendLine("Способ доставки: ${order.delivery.methodName}")
            appendLine("Сумма: ${formatMoney(order.totalMinor)}")
            appendLine("Покупатель: ${order.customerName ?: "N/A"}")
            appendLine("Телефон: ${order.customerPhone ?: "N/A"}")
            append("Время обновления: ${order.updatedAt}")
        }
    }

    private fun itemsBlock(order: Order): String {
        val lines = order.items.take(MAX_ITEMS_IN_MESSAGE).map { item ->
            "- ${item.title} x${item.quantity} = ${formatMoney(item.totalMinor)}"
        }

        val hiddenItems = order.items.size - lines.size
        if (hiddenItems > 0) {
            return lines.joinToString("\n", postfix = "\n- ...and $hiddenItems more")
        }

        return lines.joinToString("\n")
    }

    private fun formatMoney(minor: Long): String {
        return "${BigDecimal.valueOf(minor, 2).setScale(2, RoundingMode.HALF_UP)} RUB"
    }

    private fun deliveryAddress(order: Order): String {
        return order.delivery.address?.toSingleLine()
            ?: order.delivery.pickupPointAddress
            ?: order.delivery.pickupPointName
            ?: "Pickup"
    }

    companion object {
        private const val MAX_ITEMS_IN_MESSAGE = 8
    }
}
