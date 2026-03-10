package ru.foodbox.delivery.modules.notifications.application

import org.springframework.stereotype.Component
import ru.foodbox.delivery.modules.orders.domain.Order
import ru.foodbox.delivery.modules.orders.domain.OrderDeliveryType
import ru.foodbox.delivery.modules.orders.domain.OrderStatus
import java.math.BigDecimal
import java.math.RoundingMode

@Component
class TelegramOrderMessageFormatter {

    fun formatOrderCreated(order: Order): String {
        return buildString {
            appendLine("New order received")
            appendLine("Number: ${order.orderNumber}")
            appendLine("Status: ${order.status}")
            appendLine("Delivery: ${deliveryType(order.deliveryType)}")
            appendLine("Total: ${formatMoney(order.totalMinor)}")
            appendLine("Customer: ${order.customerName ?: "N/A"}")
            appendLine("Phone: ${order.customerPhone ?: "N/A"}")
            appendLine("Address: ${order.deliveryAddress ?: "Pickup"}")
            appendLine("Items:")
            append(itemsBlock(order))
            appendLine()
            append("Created at: ${order.createdAt}")
        }
    }

    fun formatOrderStatusChanged(order: Order, previousStatus: OrderStatus): String {
        return buildString {
            appendLine("Order status changed")
            appendLine("Number: ${order.orderNumber}")
            appendLine("Status: $previousStatus -> ${order.status}")
            appendLine("Delivery: ${deliveryType(order.deliveryType)}")
            appendLine("Total: ${formatMoney(order.totalMinor)}")
            appendLine("Customer: ${order.customerName ?: "N/A"}")
            appendLine("Phone: ${order.customerPhone ?: "N/A"}")
            append("Updated at: ${order.updatedAt}")
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

    private fun deliveryType(deliveryType: OrderDeliveryType): String {
        return when (deliveryType) {
            OrderDeliveryType.PICKUP -> "PICKUP"
            OrderDeliveryType.DELIVERY -> "DELIVERY"
        }
    }

    companion object {
        private const val MAX_ITEMS_IN_MESSAGE = 8
    }
}
